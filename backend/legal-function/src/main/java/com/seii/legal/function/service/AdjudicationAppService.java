package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.QuestionStatus;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.DiffItem;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.data.entity.ExpertReply;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.adjudication.CompareCommand;
import com.seii.legal.function.dto.adjudication.CompareResult;
import com.seii.legal.function.dto.adjudication.ConflictListView;
import com.seii.legal.function.dto.adjudication.ConflictQuery;
import com.seii.legal.function.dto.adjudication.ConflictView;
import com.seii.legal.function.dto.adjudication.DecideCommand;
import com.seii.legal.function.dto.adjudication.DiagramError;
import com.seii.legal.function.dto.adjudication.DiagramReviewCommand;
import com.seii.legal.function.dto.adjudication.DifferenceItemView;
import com.seii.legal.function.dto.adjudication.ExpertQuestionCommand;
import com.seii.legal.function.dto.adjudication.ExpertQuestionCreated;
import com.seii.legal.function.dto.adjudication.ExpertQuestionView;
import com.seii.legal.function.dto.adjudication.ExpertReplyCommand;
import com.seii.legal.function.dto.adjudication.FinalConfirmCommand;
import com.seii.legal.function.dto.adjudication.FinalContentView;
import com.seii.legal.function.dto.adjudication.FinalQuery;
import com.seii.legal.function.dto.adjudication.FinalVersionView;
import com.seii.legal.rule.adjudication.AdjudicationRule;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.identity.IdentityRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdjudicationAppService {

    private static final String ARBITER_FORBIDDEN = "只有本任务仲裁员可以裁定";
    private static final String EXPERT_FORBIDDEN = "只有领域专家可以回复问题";

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final TaskRepository taskRepository;
    private final AnnotationRepository annotationRepository;
    private final AdjudicationRepository adjudicationRepository;
    private final IdentityRule identityRule;
    private final AdjudicationRule adjudicationRule;
    private final DiagramRule diagramRule;

    public AdjudicationAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            TaskRepository taskRepository,
            AnnotationRepository annotationRepository,
            AdjudicationRepository adjudicationRepository,
            IdentityRule identityRule,
            AdjudicationRule adjudicationRule,
            DiagramRule diagramRule) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.taskRepository = taskRepository;
        this.annotationRepository = annotationRepository;
        this.adjudicationRepository = adjudicationRepository;
        this.identityRule = identityRule;
        this.adjudicationRule = adjudicationRule;
        this.diagramRule = diagramRule;
    }

    @Transactional(readOnly = true)
    public AppResult<CompareResult> compare(String credential, CompareCommand command) {
        AuthAttempt auth = authenticate(credential, IdentityType.ARBITER, ARBITER_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(command.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        if (!command.taskId().equals(task.taskId()) || !auth.actor().user().accountId().equals(task.arbiterId())) {
            return AppResult.fail(ARBITER_FORBIDDEN);
        }
        TaskRound round = currentRound(command.taskId(), command.documentId(), command.round());
        if (round == null) {
            return AppResult.fail("当前轮次不存在");
        }
        List<Submission> submissions = annotationRepository.findSubmissionsByRound(round.roundId(), null);
        Submission a = findByAnnotator(submissions, command.annotatorA());
        if (a == null) {
            return AppResult.fail("甲的提交不存在");
        }
        Submission b = findByAnnotator(submissions, command.annotatorB());
        if (b == null) {
            return AppResult.fail("乙的提交不存在");
        }
        Agreement agreement = adjudicationRule.difference(a.content(), b.content(), command.round());
        List<DifferenceItemView> items = new ArrayList<>();
        if (agreement.items() != null) {
            for (int i = 0; i < agreement.items().size(); i++) {
                DiffItem item = agreement.items().get(i);
                items.add(new DifferenceItemView("d" + (i + 1), item.type(), item.object(),
                        item.valueA(), item.valueB(), item.note()));
            }
        }
        CompareResult result = new CompareResult(
                round1(agreement.agreement()),
                round1(agreement.difference()),
                a.submissionId(),
                b.submissionId(),
                items,
                adjudicationRule.aboveEighty(agreement));
        return AppResult.ok(result, List.of("进入裁定冲突", "切换标注员对"));
    }

    @Transactional(readOnly = true)
    public AppResult<ConflictListView> conflicts(String credential, ConflictQuery query) {
        AuthAttempt auth = authenticate(credential, IdentityType.ARBITER, ARBITER_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(query.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        if (!auth.actor().user().accountId().equals(task.arbiterId())) {
            return AppResult.fail(ARBITER_FORBIDDEN);
        }
        TaskRound round = currentRound(query.taskId(), query.documentId(), query.round());
        if (round == null) {
            return AppResult.fail("当前轮次不存在");
        }
        List<AdjudicationRecord> decisions = adjudicationRepository.findDecisions(
                query.taskId(), query.documentId(), query.round());
        List<ConflictView> conflicts = new ArrayList<>();
        Set<String> decidedIds = decisions.stream()
                .map(AdjudicationRecord::conflictId)
                .collect(Collectors.toSet());
        for (AdjudicationRecord decision : decisions) {
            conflicts.add(new ConflictView(decision.conflictId(), decision.differenceType(),
                    decision.differenceObject(), decision.valueA(), decision.valueB(),
                    decision.method(), decision.result(), true));
        }
        List<Submission> submissions = annotationRepository.findSubmissionsByRound(round.roundId(), null);
        int undecided = 0;
        if (submissions.size() >= 2) {
            List<DiffItem> diffs = adjudicationRule.classify(
                    submissions.get(0).content(), submissions.get(1).content(), query.round());
            if (diffs != null) {
                for (int i = 0; i < diffs.size(); i++) {
                    String conflictId = "d" + (i + 1);
                    if (decidedIds.contains(conflictId)) {
                        continue;
                    }
                    DiffItem item = diffs.get(i);
                    conflicts.add(new ConflictView(conflictId, item.type(), item.object(),
                            item.valueA(), item.valueB(), null, null, false));
                    undecided++;
                }
            }
        }
        return AppResult.ok(new ConflictListView(conflicts, conflicts.size() - undecided, undecided),
                List.of("裁定冲突", "提交领域专家"));
    }

    @Transactional
    public AppResult<Void> decide(String credential, DecideCommand command) {
        AuthAttempt auth = authenticate(credential, IdentityType.ARBITER, ARBITER_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(command.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        if (!auth.actor().user().accountId().equals(task.arbiterId())) {
            return AppResult.fail(ARBITER_FORBIDDEN);
        }
        TaskRound round = currentRound(command.taskId(), command.documentId(), command.round());
        if (round == null) {
            return AppResult.fail("当前轮次不存在");
        }
        annotationRepository.findSubmissionsByRound(round.roundId(), null);
        AdjudicationRecord record = new AdjudicationRecord(
                null,
                command.taskId(),
                command.documentId(),
                command.round(),
                round.roundId(),
                command.conflictId(),
                command.differenceType(),
                command.differenceObject(),
                command.valueA(),
                command.valueB(),
                command.method(),
                command.result(),
                auth.actor().user().accountId(),
                LocalDateTime.now(),
                null,
                null,
                null,
                command.expertReplyId());
        adjudicationRepository.saveDecision(record);
        return AppResult.ok(null, List.of("继续裁定", "查看未裁定项"));
    }

    @Transactional
    public AppResult<ExpertQuestionCreated> askExpert(String credential, ExpertQuestionCommand command) {
        AuthAttempt auth = authenticate(credential, IdentityType.ARBITER, ARBITER_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(command.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        if (!auth.actor().user().accountId().equals(task.arbiterId())) {
            return AppResult.fail(ARBITER_FORBIDDEN);
        }
        if (command.expertId() == null || command.expertId().isBlank()) {
            return AppResult.fail("请选择领域专家");
        }
        String documentId = task.documentIds() == null || task.documentIds().isEmpty()
                ? null : task.documentIds().get(0);
        TaskRound round = finalRound(command.taskId(), documentId);
        if (round != null) {
            annotationRepository.findSubmissionsByRound(round.roundId(), null);
        }
        ExpertQuestion question = new ExpertQuestion(
                null,
                command.taskId(),
                documentId,
                command.conflictId(),
                command.expertId(),
                questionContent(command),
                QuestionStatus.PENDING,
                LocalDateTime.now(),
                null);
        String questionId = adjudicationRepository.saveExpertQuestion(question);
        return AppResult.ok(new ExpertQuestionCreated(questionId), List.of("查看待回复问题"));
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<ExpertQuestionView>> expertQuestions(
            String credential, String status, Integer page, Integer size) {
        AuthAttempt auth = authenticate(credential, IdentityType.EXPERT, EXPERT_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        QuestionStatus questionStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                questionStatus = QuestionStatus.valueOf(status);
            } catch (IllegalArgumentException ex) {
                return AppResult.fail("问题状态不正确");
            }
        }
        int pageNo = page == null || page <= 0 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : size;
        PageResult<ExpertQuestion> result = adjudicationRepository.findExpertQuestions(
                auth.actor().user().accountId(), questionStatus, pageNo, pageSize);
        List<ExpertQuestionView> records = result.records().stream()
                .map(this::toView)
                .toList();
        return AppResult.ok(new PageResult<>(records, result.total(), result.page(), result.size()));
    }

    @Transactional
    public AppResult<Void> replyExpert(String credential, ExpertReplyCommand command) {
        AuthAttempt auth = authenticate(credential, IdentityType.EXPERT, EXPERT_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        ExpertQuestion question = adjudicationRepository.findExpertQuestion(command.questionId()).orElse(null);
        if (question == null) {
            return AppResult.fail("问题不存在");
        }
        if (!auth.actor().user().accountId().equals(question.expertId())) {
            return AppResult.fail("只有被指定的专家可以回复");
        }
        TaskRound round = finalRound(question.taskId(), question.documentId());
        if (round != null) {
            annotationRepository.findSubmissionsByRound(round.roundId(), null);
        }
        QuestionStatus status = replyStatus(command);
        ExpertReply reply = new ExpertReply(
                null,
                command.questionId(),
                auth.actor().user().accountId(),
                replyContent(command),
                LocalDateTime.now());
        adjudicationRepository.saveExpertReply(reply);
        adjudicationRepository.updateQuestionStatus(command.questionId(), status);
        return AppResult.ok(null, List.of("查看待回复问题"));
    }

    @Transactional
    public AppResult<Void> reviewDiagram(String credential, DiagramReviewCommand command) {
        AuthAttempt auth = authenticate(credential, IdentityType.ARBITER, ARBITER_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(command.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        if (!auth.actor().user().accountId().equals(task.arbiterId())) {
            return AppResult.fail(ARBITER_FORBIDDEN);
        }
        TaskRound round = finalRound(command.taskId(), command.documentId());
        if (round != null) {
            annotationRepository.findSubmissionsByRound(round.roundId(), null);
        }
        Diagram diagram = annotationRepository.findDiagram(command.diagramId()).orElse(null);
        if (diagram == null) {
            return AppResult.fail("图示不存在");
        }
        List<Relation> relations = annotationRepository.findRelationsByVersion(diagram.relationVersionId());
        List<String> consistency = diagramRule.checkConsistency(diagram, relations);
        if (Boolean.TRUE.equals(command.passed()) && consistency != null && !consistency.isEmpty()) {
            return AppResult.fail("图示一致性检查未通过：" + String.join("；", consistency));
        }
        List<String> errors = new ArrayList<>();
        if (command.errors() != null) {
            for (DiagramError error : command.errors()) {
                errors.add(error.position() + ":" + error.type() + ":" + error.note());
            }
        }
        if (errors.isEmpty() && consistency != null) {
            errors.addAll(consistency);
        }
        DiagramReview review = new DiagramReview(
                null,
                command.taskId(),
                command.documentId(),
                command.diagramId(),
                auth.actor().user().accountId(),
                LocalDateTime.now(),
                Boolean.TRUE.equals(command.passed()) ? "通过" : "不通过",
                errors);
        adjudicationRepository.saveDiagramReview(review);
        return AppResult.ok(null, List.of("确认最终版本"));
    }

    @Transactional(readOnly = true)
    public AppResult<FinalVersionView> finalVersion(String credential, FinalQuery query) {
        AuthAttempt auth = authenticate(credential, null, null);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(query.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        LoginUser user = auth.actor().user();
        if (!identityRule.hasRole(auth.actor().account(), user.identity()) || !involvedInTask(task, user)) {
            return AppResult.fail("当前身份不能查看最终版本");
        }
        TaskRound round = finalRound(query.taskId(), query.documentId());
        if (round == null) {
            return AppResult.fail("当前轮次不存在");
        }
        annotationRepository.findSubmissionsByRound(round.roundId(), null);
        Optional<FinalVersion> version = adjudicationRepository.findFinalVersionByRound(round.roundId(), VersionType.FINAL);
        if (version.isEmpty()) {
            version = adjudicationRepository.findFinalVersion(query.taskId(), query.documentId(), VersionType.FINAL);
        }
        if (version.isEmpty()) {
            version = adjudicationRepository.findFinalVersion(query.taskId(), query.documentId());
        }
        if (version.isEmpty()) {
            return AppResult.fail("最终版本不存在");
        }
        FinalVersion value = version.get();
        FinalContentView content = new FinalContentView(
                value.roundId(),
                value.versionType() == null ? null : value.versionType().name(),
                value.cutVersionId(),
                value.elementVersionId(),
                value.relationVersionId(),
                value.diagramId());
        return AppResult.ok(new FinalVersionView(content, value.guideVersion(), value.confirmedBy(),
                value.confirmedAt(), value.status()));
    }

    @Transactional
    public AppResult<Void> finalConfirm(String credential, FinalConfirmCommand command) {
        AuthAttempt auth = authenticate(credential, IdentityType.ARBITER, ARBITER_FORBIDDEN);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(command.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        if (!auth.actor().user().accountId().equals(task.arbiterId())) {
            return AppResult.fail(ARBITER_FORBIDDEN);
        }
        if (task.status() == TaskStatus.CONFIRMED) {
            return AppResult.fail("任务已确认");
        }
        TaskRound round = finalRound(command.taskId(), command.documentId());
        if (round == null) {
            return AppResult.fail("当前轮次不存在");
        }
        annotationRepository.findSubmissionsByRound(round.roundId(), null);
        List<AdjudicationRecord> decisions = adjudicationRepository.findDecisionsByRound(round.roundId());
        List<DiagramReview> reviews = adjudicationRepository.findDiagramReviews(
                command.taskId(), command.documentId());
        CutVersion unifiedCut = annotationRepository.findCutVersionByRound(round.roundId(), null).orElse(null);
        List<String> issues = adjudicationRule.checkFinalVersion(decisions, reviews, unifiedCut);
        if (issues != null && !issues.isEmpty()) {
            return AppResult.fail("存在未完成项：" + String.join("；", issues));
        }
        LocalDateTime now = LocalDateTime.now();
        FinalVersion version = new FinalVersion(
                null,
                command.taskId(),
                command.documentId(),
                round.roundId(),
                VersionType.FINAL,
                unifiedCut == null ? null : unifiedCut.versionId(),
                null,
                null,
                null,
                task.guideVersion(),
                auth.actor().user().accountId(),
                now,
                "CONFIRMED");
        adjudicationRepository.saveFinalVersion(version);
        adjudicationRepository.confirmTask(command.taskId(), auth.actor().user().accountId(), now);
        return AppResult.ok(null, List.of("导出数据"));
    }

    private AuthAttempt authenticate(String credential, IdentityType required, String forbidden) {
        LoginUser user = sessionRepository.findByCredential(credential).orElse(null);
        if (user == null) {
            return new AuthAttempt(null, "登录已失效，请重新登录");
        }
        Account account = accountRepository.findById(user.accountId()).orElse(null);
        if (account == null) {
            return new AuthAttempt(null, "账号不存在");
        }
        if (!identityRule.isEnabled(account)) {
            return new AuthAttempt(null, "账号已停用，请联系管理员");
        }
        if (required != null
                && (user.identity() != required || !identityRule.hasRole(account, required))) {
            return new AuthAttempt(null, forbidden);
        }
        return new AuthAttempt(new Actor(user, account), null);
    }

    private TaskRound currentRound(String taskId, String documentId, RoundType round) {
        if (round == null) {
            return null;
        }
        return taskRepository.findCurrentRound(taskId, documentId, round).orElse(null);
    }

    private TaskRound finalRound(String taskId, String documentId) {
        TaskRound structured = taskRepository.findCurrentRound(taskId, documentId, RoundType.STRUCTURED)
                .orElse(null);
        if (structured != null) {
            return structured;
        }
        return taskRepository.findCurrentRound(taskId, documentId, RoundType.CUTTING).orElse(null);
    }

    private Submission findByAnnotator(List<Submission> submissions, String annotatorId) {
        if (submissions == null) {
            return null;
        }
        return submissions.stream()
                .filter(item -> item.annotatorId().equals(annotatorId))
                .findFirst()
                .orElse(null);
    }

    private boolean involvedInTask(AnnotationTask task, LoginUser user) {
        return switch (user.identity()) {
            case ARBITER -> user.accountId().equals(task.arbiterId());
            case TASK_MANAGER -> user.accountId().equals(task.publisherId());
            case ANNOTATOR -> task.annotatorIds() != null && task.annotatorIds().contains(user.accountId());
            default -> false;
        };
    }

    private static QuestionStatus replyStatus(ExpertReplyCommand command) {
        if (Boolean.TRUE.equals(command.unanswerable())) {
            return QuestionStatus.UNANSWERABLE;
        }
        if (Boolean.TRUE.equals(command.needMore())) {
            return QuestionStatus.NEED_MORE;
        }
        return QuestionStatus.REPLIED;
    }

    private static String questionContent(ExpertQuestionCommand command) {
        List<String> parts = new ArrayList<>();
        if (command.guideClause() != null && !command.guideClause().isBlank()) {
            parts.add("指南条款：" + command.guideClause());
        }
        if (command.conflictLabel() != null && !command.conflictLabel().isBlank()) {
            parts.add("冲突标签：" + command.conflictLabel());
        }
        if (command.annotatorResults() != null && !command.annotatorResults().isBlank()) {
            parts.add("标注结果：" + command.annotatorResults());
        }
        if (command.note() != null && !command.note().isBlank()) {
            parts.add(command.note());
        }
        return String.join("；", parts);
    }

    private static String replyContent(ExpertReplyCommand command) {
        List<String> parts = new ArrayList<>();
        if (command.conclusion() != null && !command.conclusion().isBlank()) {
            parts.add(command.conclusion());
        }
        if (command.reason() != null && !command.reason().isBlank()) {
            parts.add(command.reason());
        }
        if (Boolean.TRUE.equals(command.needMore())) {
            parts.add("需要补充材料");
        }
        if (Boolean.TRUE.equals(command.unanswerable())) {
            parts.add("无法判断");
        }
        return String.join("；", parts);
    }

    private ExpertQuestionView toView(ExpertQuestion question) {
        return new ExpertQuestionView(question.questionId(), question.taskId(), question.documentId(),
                question.conflictId(), question.expertId(), question.content(), question.status(),
                question.askedAt(), question.repliedAt());
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Actor(LoginUser user, Account account) {
    }

    private record AuthAttempt(Actor actor, String error) {
    }
}
