package com.seii.legal.function.service;

import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.QuestionStatus;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.data.entity.ExpertReply;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.AdjudicationDtos;
import com.seii.legal.rule.adjudication.AdjudicationRule;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.task.TaskRule;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 裁定与领域专家功能层服务（ADJ-01…09）。
 */
@Service
@RequiredArgsConstructor
public class AdjudicationAppService {

    private static final Set<IdentityType> ARBITER_ONLY = Set.of(IdentityType.ARBITER);
    private static final Set<IdentityType> EXPERT_ONLY = Set.of(IdentityType.EXPERT);
    private static final Set<IdentityType> FINAL_VIEW_ROLES = Set.of(
            IdentityType.ARBITER, IdentityType.TASK_MANAGER, IdentityType.ANNOTATOR, IdentityType.ADMIN);

    private final AuthActionTemplate authTemplate;
    private final TaskRepository taskRepository;
    private final AccountRepository accountRepository;
    private final AnnotationRepository annotationRepository;
    private final AdjudicationRepository adjudicationRepository;
    private final AdjudicationRule adjudicationRule;
    private final DiagramRule diagramRule;
    private final TaskRule taskRule;
    private final AnnotationPayloadAdapter payloadAdapter;
    private final AllowedActionResolver actionResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AppResult<AdjudicationDtos.CompareResponse> compare(
            String credential, AdjudicationDtos.CompareRequest request) {
        return authTemplate.execute(credential, ARBITER_ONLY, request.taskId(), null, "只有本任务仲裁员可以裁定", ctx -> {
            if (ctx.scope() != DataScope.TASK_SUBMISSIONS) {
                return AppResult.fail("当前身份不能查看这个任务的提交");
            }
            requireOwnedArbiterTask(request.taskId(), ctx.user().accountId());
            List<Submission> submissions = annotationRepository.findSubmissions(
                    request.taskId(), request.documentId(), request.round(), null);
            Submission a = submissions.stream()
                    .filter(s -> request.annotatorA().equals(s.annotatorId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("甲的提交不存在"));
            Submission b = submissions.stream()
                    .filter(s -> request.annotatorB().equals(s.annotatorId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("乙的提交不存在"));

            Agreement agreement = adjudicationRule.difference(a.content(), b.content(), request.round());
            AdjudicationDtos.CompareResponse response = new AdjudicationDtos.CompareResponse(
                    agreement.agreement(),
                    agreement.difference(),
                    resolveUserName(request.annotatorA()),
                    resolveUserName(request.annotatorB()),
                    agreement.items()
            );
            return AppResult.ok(response, List.of("进入裁定冲突", "切换标注员对"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<AdjudicationDtos.ConflictsResponse> conflicts(
            String credential, String taskId, String documentId, RoundType round) {
        return authTemplate.execute(credential, ARBITER_ONLY, taskId, null, "只有本任务仲裁员可以查看冲突", ctx -> {
            requireOwnedArbiterTask(taskId, ctx.user().accountId());
            List<AdjudicationRecord> records = adjudicationRepository.findDecisions(taskId, documentId, round);
            long decided = records.stream()
                    .filter(r -> r.result() != null && !r.result().isBlank())
                    .count();
            long undecided = records.size() - decided;
            return AppResult.ok(new AdjudicationDtos.ConflictsResponse(records, decided, undecided));
        });
    }

    @Transactional
    public AppResult<Void> decide(String credential, AdjudicationDtos.DecideRequest request) {
        return authTemplate.execute(credential, ARBITER_ONLY, request.taskId(), null, "只有本任务仲裁员可以裁定", ctx -> {
            AnnotationTask task = requireOwnedArbiterTask(request.taskId(), ctx.user().accountId());
            AdjudicationRecord record = new AdjudicationRecord(
                    null,
                    request.taskId(),
                    request.documentId(),
                    request.round(),
                    request.conflictId(),
                    request.differenceType(),
                    request.differenceObject(),
                    request.valueA(),
                    request.valueB(),
                    request.method(),
                    request.result(),
                    ctx.user().accountId(),
                    TimeFormats.now(),
                    request.expertReplyId()
            );
            adjudicationRepository.saveDecision(record);
            taskRule.nextStatus(task.status(), TaskRule.TaskAction.DECIDE);
            return AppResult.ok(null, List.of("继续裁定", "查看未裁定项"));
        });
    }

    @Transactional
    public AppResult<AdjudicationDtos.ExpertQuestionResponse> submitExpertQuestion(
            String credential, AdjudicationDtos.ExpertQuestionRequest request) {
        return authTemplate.execute(credential, ARBITER_ONLY, request.taskId(), null, "只有本任务仲裁员可以提交专家裁决", ctx -> {
            requireOwnedArbiterTask(request.taskId(), ctx.user().accountId());
            ExpertQuestion question = new ExpertQuestion(
                    null,
                    request.taskId(),
                    request.documentId(),
                    request.conflictId(),
                    request.guideClause(),
                    request.conflictLabel(),
                    request.annotatorResults(),
                    ctx.user().accountId(),
                    request.expertId(),
                    QuestionStatus.PENDING,
                    request.note(),
                    TimeFormats.now()
            );
            String questionId = adjudicationRepository.saveExpertQuestion(question);
            eventPublisher.publishEvent(new DomainNotificationEvent(
                    List.of(request.expertId()),
                    "EXPERT_QUESTION",
                    "新的领域专家裁决问题",
                    "任务 " + request.taskId() + " 有新的分歧问题待回复：" + request.conflictLabel(),
                    false
            ));
            return AppResult.ok(new AdjudicationDtos.ExpertQuestionResponse(questionId), List.of("继续裁定"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<ExpertQuestion>> listExpertQuestions(
            String credential, QuestionStatus status, Integer page, Integer size) {
        return authTemplate.execute(credential, EXPERT_ONLY, null, null, "仅领域专家可查看待回复问题", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            PageResult<ExpertQuestion> result = adjudicationRepository.findExpertQuestions(
                    ctx.user().accountId(), status, p, s);
            return AppResult.ok(result);
        });
    }

    @Transactional
    public AppResult<Void> replyExpertQuestion(
            String credential, AdjudicationDtos.ExpertReplyRequest request) {
        return authTemplate.execute(credential, EXPERT_ONLY, null, null, "仅领域专家可回复问题", ctx -> {
            ExpertQuestion question = adjudicationRepository.findExpertQuestion(request.questionId())
                    .orElseThrow(() -> new BusinessException("专家问题不存在"));
            if (!ctx.user().accountId().equals(question.expertId())) {
                return AppResult.fail("仅指定领域专家可回复该问题");
            }
            QuestionStatus targetStatus = resolveReplyStatus(request);
            ExpertReply reply = new ExpertReply(
                    null,
                    request.questionId(),
                    request.conclusion(),
                    request.reason(),
                    ctx.user().accountId(),
                    TimeFormats.now(),
                    request.referenceBasis(),
                    targetStatus
            );
            adjudicationRepository.saveExpertReply(reply);
            adjudicationRepository.updateQuestionStatus(request.questionId(), targetStatus);
            eventPublisher.publishEvent(new DomainNotificationEvent(
                    List.of(question.submitterId()),
                    "EXPERT_REPLY",
                    "领域专家已回复问题",
                    "问题 " + request.questionId() + " 状态更新为 " + targetStatus + "：" + (request.conclusion() == null ? "" : request.conclusion()),
                    false
            ));
            return AppResult.ok(null, List.of("返回待回复问题列表"));
        });
    }

    @Transactional
    public AppResult<Void> reviewDiagram(
            String credential, AdjudicationDtos.DiagramReviewRequest request) {
        return authTemplate.execute(credential, ARBITER_ONLY, request.taskId(), null, "只有本任务仲裁员可以审查图示", ctx -> {
            AnnotationTask task = requireOwnedArbiterTask(request.taskId(), ctx.user().accountId());
            Optional<Diagram> diagram = annotationRepository.findDiagram(request.diagramId());
            if (diagram.isPresent() && diagram.get().relationVersionId() != null) {
                List<Relation> relations = annotationRepository.findRelations(diagram.get().relationVersionId());
                diagramRule.checkConsistency(diagram.get(), relations);
            }
            DiagramReview review = new DiagramReview(
                    null,
                    request.taskId(),
                    request.documentId(),
                    request.diagramId(),
                    ctx.user().accountId(),
                    TimeFormats.now(),
                    request.passed(),
                    request.errors() == null ? List.of() : request.errors()
            );
            adjudicationRepository.saveDiagramReview(review);
            if (!request.passed() && task.annotatorIds() != null && !task.annotatorIds().isEmpty()) {
                eventPublisher.publishEvent(new DomainNotificationEvent(
                        task.annotatorIds(),
                        "DIAGRAM_REVIEW_FAILED",
                        "论证图示审查未通过",
                        "任务「" + task.name() + "」的图示审查存在问题，请关注裁定结果。",
                        false
                ));
            }
            return AppResult.ok(null, List.of("确认最终版本"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<AdjudicationDtos.FinalVersionResponse> viewFinalVersion(
            String credential, String taskId, String documentId, VersionType versionType) {
        return authTemplate.execute(credential, FINAL_VIEW_ROLES, taskId, null, "无权查看最终版本", ctx -> {
            AnnotationTask task = requireReadableTask(taskId, ctx.user().accountId(), ctx.user().identity());
            Optional<FinalVersion> fvOpt = (versionType != null)
                    ? adjudicationRepository.findFinalVersion(taskId, documentId, versionType)
                    : adjudicationRepository.findFinalVersion(taskId, documentId);
            FinalVersion fv = fvOpt.orElseThrow(() -> new BusinessException("最终版本尚未形成"));
            AdjudicationDtos.FinalVersionResponse dto = new AdjudicationDtos.FinalVersionResponse(
                    payloadAdapter.fromStorageJson(fv.contentJson()),
                    fv.guideVersion(),
                    resolveUserName(fv.confirmedBy()),
                    TimeFormats.format(fv.confirmedAt()),
                    fv.status()
            );
            return AppResult.ok(dto, actionResolver.resolve(task.status(), ctx.user().identity()));
        });
    }

    @Transactional
    public AppResult<Void> confirmFinalVersion(
            String credential, AdjudicationDtos.FinalConfirmRequest request) {
        return authTemplate.execute(credential, ARBITER_ONLY, request.taskId(), null, "只有本任务仲裁员可以确认最终版本", ctx -> {
            AnnotationTask task = requireOwnedArbiterTask(request.taskId(), ctx.user().accountId());
            List<AdjudicationRecord> decisions = adjudicationRepository.findDecisions(
                    request.taskId(), request.documentId(), RoundType.STRUCTURED);
            List<DiagramReview> reviews = adjudicationRepository.findDiagramReviews(
                    request.taskId(), request.documentId());
            CutVersion unifiedCut = annotationRepository.findCutVersion(
                    request.taskId(), request.documentId(), ctx.user().accountId(), RoundType.CUTTING).orElse(null);

            List<String> missing = adjudicationRule.checkFinalVersion(decisions, reviews, unifiedCut);
            if (missing != null && !missing.isEmpty()) {
                return AppResult.fail("尚不能确认最终版本：" + String.join("、", missing));
            }

            LocalDateTime now = TimeFormats.now();
            Optional<FinalVersion> existingCut = adjudicationRepository.findFinalVersion(
                    request.taskId(), request.documentId(), VersionType.CUTTING);
            String cutVersionId = unifiedCut != null
                    ? unifiedCut.versionId()
                    : existingCut.map(FinalVersion::cutVersionId).orElse(null);

            FinalVersion finalVersion = new FinalVersion(
                    null,
                    request.taskId(),
                    request.documentId(),
                    VersionType.FINAL,
                    cutVersionId,
                    null,
                    null,
                    reviews.isEmpty() ? null : reviews.get(reviews.size() - 1).diagramId(),
                    null,
                    task.guideVersion(),
                    ctx.user().accountId(),
                    now,
                    "CONFIRMED"
            );
            adjudicationRepository.saveFinalVersion(finalVersion);
            adjudicationRepository.confirmTask(request.taskId(), ctx.user().accountId(), now);

            eventPublisher.publishEvent(new DomainNotificationEvent(
                    List.of(task.publisherId()),
                    "TASK_CONFIRMED",
                    "任务最终版本已确认",
                    "任务「" + task.name() + "」已完成裁定并确认最终版本，可进行结果导出。",
                    false
            ));
            return AppResult.ok(null, actionResolver.resolve(TaskStatus.CONFIRMED, IdentityType.ARBITER));
        });
    }

    private QuestionStatus resolveReplyStatus(AdjudicationDtos.ExpertReplyRequest request) {
        if (Boolean.TRUE.equals(request.unanswerable())) {
            return QuestionStatus.UNANSWERABLE;
        }
        if (Boolean.TRUE.equals(request.needMore())) {
            return QuestionStatus.NEED_MORE;
        }
        return QuestionStatus.REPLIED;
    }

    private AnnotationTask requireOwnedArbiterTask(String taskId, String arbiterId) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!arbiterId.equals(task.arbiterId())) {
            throw new BusinessException("只有本任务仲裁员可以裁定");
        }
        return task;
    }

    private AnnotationTask requireReadableTask(String taskId, String accountId, IdentityType identity) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (identity == IdentityType.ADMIN) {
            return task;
        }
        if (identity == IdentityType.ARBITER && accountId.equals(task.arbiterId())) {
            return task;
        }
        if (identity == IdentityType.TASK_MANAGER && accountId.equals(task.publisherId())) {
            return task;
        }
        if (identity == IdentityType.ANNOTATOR && task.annotatorIds() != null && task.annotatorIds().contains(accountId)) {
            return task;
        }
        throw new BusinessException("无权访问该任务最终版本");
    }

    private String resolveUserName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }
}
