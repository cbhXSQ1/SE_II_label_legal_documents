package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.Draft;
import com.seii.legal.data.entity.Element;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.DataConflictException;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.annotation.LoadAnnotationView;
import com.seii.legal.function.dto.annotation.SaveDraftCommand;
import com.seii.legal.function.dto.annotation.SaveDraftView;
import com.seii.legal.function.dto.annotation.SubmitCommand;
import com.seii.legal.function.dto.annotation.SubmitView;
import com.seii.legal.function.support.AnnotationPayloadAdapter;
import com.seii.legal.function.support.AnnotationPayloadAdapter.AnnotationContent;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.annotation.ElementRule;
import com.seii.legal.rule.annotation.RelationRule;
import com.seii.legal.rule.annotation.SegmentationRule;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnotationAppService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> PROPOSITION_TYPES = Set.of(
            "SF", "GF", "SM", "GM", "SM-C", "GM-L", "GM-I", "GM-U", "GM-M", "GM-O");

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final TaskRepository taskRepository;
    private final AnnotationRepository annotationRepository;
    private final IdentityRule identityRule;
    private final TaskRule taskRule;
    private final SegmentationRule segmentationRule;
    private final ElementRule elementRule;
    private final RelationRule relationRule;
    private final DiagramRule diagramRule;
    private final AnnotationPayloadAdapter payloadAdapter;

    public AnnotationAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            TaskRepository taskRepository,
            AnnotationRepository annotationRepository,
            IdentityRule identityRule,
            TaskRule taskRule,
            SegmentationRule segmentationRule,
            ElementRule elementRule,
            RelationRule relationRule,
            DiagramRule diagramRule,
            AnnotationPayloadAdapter payloadAdapter) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.taskRepository = taskRepository;
        this.annotationRepository = annotationRepository;
        this.identityRule = identityRule;
        this.taskRule = taskRule;
        this.segmentationRule = segmentationRule;
        this.elementRule = elementRule;
        this.relationRule = relationRule;
        this.diagramRule = diagramRule;
        this.payloadAdapter = payloadAdapter;
    }

    @Transactional(readOnly = true)
    public AppResult<LoadAnnotationView> load(String credential, String taskId, String documentId) {
        AuthCheck auth = authenticate(credential, taskId);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = auth.actor().task();
        String accountId = auth.actor().user().accountId();
        Optional<TaskRound> round = currentRound(task, documentId, null);
        Optional<Draft> draft = annotationRepository.findDraft(taskId, documentId, accountId);
        List<Submission> submissions = round
                .map(value -> annotationRepository.findSubmissionsByRound(value.roundId(), accountId))
                .orElse(List.of());
        Submission submission = submissions.isEmpty() ? null : submissions.get(submissions.size() - 1);

        AnnotationContent content;
        if (submission != null) {
            content = payloadAdapter.fromStorageJson(submission.content());
        } else {
            content = draft
                    .map(value -> payloadAdapter.fromDraftJson(value.cutJson(), value.elementsJson(),
                            value.relationsJson(), value.diagramJson()))
                    .orElseGet(payloadAdapter::empty);
        }
        String roundName = round.map(value -> value.type().name()).orElse(null);
        return AppResult.ok(new LoadAnnotationView(content, submission != null, roundName));
    }

    @Transactional
    public AppResult<SaveDraftView> saveDraft(String credential, SaveDraftCommand command) {
        AuthCheck auth = authenticate(credential, command.taskId());
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = auth.actor().task();
        String accountId = auth.actor().user().accountId();
        Optional<TaskRound> round = currentRound(task, command.documentId(), null);
        if (round.isEmpty()) {
            return AppResult.fail("当前轮次不存在");
        }
        TaskRound current = round.get();
        List<Submission> existing = annotationRepository.findSubmissionsByRound(
                current.roundId(), accountId);
        if (!existing.isEmpty()) {
            return AppResult.fail("本轮已经提交");
        }
        AnnotationContent content = payloadAdapter.merge(
                command.content(),
                new AnnotationContent(command.pieces(), command.segments(),
                        command.relations(), command.diagram()));
        LocalDateTime now = LocalDateTime.now();
        Draft draft = new Draft(
                command.taskId(),
                command.documentId(),
                accountId,
                current.roundId(),
                payloadAdapter.listToJson(content.pieces()),
                payloadAdapter.listToJson(content.segments()),
                payloadAdapter.listToJson(content.relations()),
                payloadAdapter.listToJson(content.diagram()),
                now);
        annotationRepository.saveDraft(draft);
        return AppResult.ok(new SaveDraftView(format(now)));
    }

    @Transactional
    public AppResult<SubmitView> submit(String credential, SubmitCommand command) {
        AuthCheck auth = authenticate(credential, command.taskId());
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        if (command.requestId() == null || command.requestId().isBlank()) {
            return AppResult.fail("缺少请求标识");
        }
        AnnotationTask task = auth.actor().task();
        String accountId = auth.actor().user().accountId();
        Optional<TaskRound> round = command.round() != null
                ? taskRepository.findCurrentRound(command.taskId(), command.documentId(), command.round())
                : currentRound(task, command.documentId(), null);
        if (round.isEmpty()) {
            return AppResult.fail("当前轮次不存在");
        }
        TaskRound current = round.get();
        LocalDateTime now = LocalDateTime.now();
        if (!taskRule.canSubmit(task.deadline(), now)) {
            return AppResult.fail("已过截止时间，不能提交");
        }
        if (taskRule.nextStatus(task.status(), TaskRule.TaskAction.SUBMIT) == null) {
            return AppResult.fail("当前任务状态不能提交");
        }
        AnnotationContent content = payloadAdapter.merge(
                command.content(),
                new AnnotationContent(command.pieces(), command.segments(),
                        command.relations(), command.diagram()));
        List<String> unfinished = unfinished(current, content);
        if (!unfinished.isEmpty()) {
            return AppResult.fail("未完成项：" + String.join("；", unfinished));
        }
        Submission submission = new Submission(
                null,
                command.taskId(),
                command.documentId(),
                current.type(),
                current.roundId(),
                accountId,
                now,
                task.guideVersion(),
                payloadAdapter.toStorageJson(content, null),
                SubmissionStatus.SUBMITTED,
                null);
        String submissionId;
        try {
            submissionId = annotationRepository.saveSubmission(submission, command.requestId());
        } catch (DataConflictException e) {
            return AppResult.fail(e.getMessage());
        }
        List<Submission> roundSubmissions = annotationRepository.findSubmissionsByRound(current.roundId(), null);
        taskRule.progress(task.taskId(), roundSubmissions == null ? List.of() : roundSubmissions);
        return AppResult.ok(new SubmitView(submissionId, format(now)));
    }

    private AuthCheck authenticate(String credential, String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return new AuthCheck(null, "任务不存在");
        }
        var user = sessionRepository.findByCredential(credential).orElse(null);
        if (user == null) {
            return new AuthCheck(null, "登录已失效，请重新登录");
        }
        Account account = accountRepository.findById(user.accountId()).orElse(null);
        if (account == null) {
            return new AuthCheck(null, "账号不存在");
        }
        if (!identityRule.isEnabled(account)) {
            return new AuthCheck(null, "账号已停用，请联系管理员");
        }
        if (user.identity() != IdentityType.ANNOTATOR
                || !identityRule.hasRole(account, IdentityType.ANNOTATOR)) {
            return new AuthCheck(null, "只有本任务标注员可以标注");
        }
        AnnotationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return new AuthCheck(null, "任务不存在");
        }
        if (task.annotatorIds() == null || !task.annotatorIds().contains(user.accountId())) {
            return new AuthCheck(null, "只有本任务标注员可以标注");
        }
        return new AuthCheck(new Actor(user, account, task), null);
    }

    private Optional<TaskRound> currentRound(AnnotationTask task, String documentId, RoundType requested) {
        if (requested != null) {
            return taskRepository.findCurrentRound(task.taskId(), documentId, requested);
        }
        for (RoundType type : roundOrder(task.status())) {
            Optional<TaskRound> round = taskRepository.findCurrentRound(task.taskId(), documentId, type);
            if (round.isPresent()) {
                return round;
            }
        }
        return Optional.empty();
    }

    private static List<RoundType> roundOrder(TaskStatus status) {
        if (status == null) {
            return List.of(RoundType.CUTTING, RoundType.STRUCTURED);
        }
        return switch (status) {
            case CUTTING_CONFIRMED, STRUCTURED_PENDING, CONFIRMED ->
                    List.of(RoundType.STRUCTURED, RoundType.CUTTING);
            default -> List.of(RoundType.CUTTING, RoundType.STRUCTURED);
        };
    }

    private List<String> unfinished(TaskRound round, AnnotationContent content) {
        List<String> problems = new ArrayList<>();
        if (round.type() == RoundType.CUTTING) {
            if (content.segments().isEmpty()) {
                problems.add("尚未切分");
                return problems;
            }
            List<Integer> uncut = segmentationRule.findUncut(cutPoints(content), textOf(content));
            if (uncut != null && !uncut.isEmpty()) {
                problems.add("存在未切分位置");
            }
            return problems;
        }
        List<Element> elements = elementsOf(content);
        List<String> unassigned = elementRule.findUnassigned(elements);
        if (unassigned != null && !unassigned.isEmpty()) {
            problems.add("存在未分配要素：" + String.join(",", unassigned));
        }
        List<Relation> relations = relationsOf(content);
        List<String> propositionIds = elements.stream()
                .filter(Element::proposition)
                .map(Element::elementId)
                .toList();
        List<String> isolated = relationRule.findIsolated(relations, propositionIds);
        if (isolated != null && !isolated.isEmpty()) {
            problems.add("存在孤立命题：" + String.join(",", isolated));
        }
        List<String> diagramErrors = diagramRule.checkConsistency(diagramOf(content), relations);
        if (diagramErrors != null && !diagramErrors.isEmpty()) {
            problems.add("图示不一致：" + String.join("；", diagramErrors));
        }
        return problems;
    }

    private static String textOf(AnnotationContent content) {
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> piece : content.pieces()) {
            text.append(stringOf(piece.get("text")));
        }
        if (text.isEmpty()) {
            for (Map<String, Object> segment : content.segments()) {
                text.append(stringOf(segment.get("text")));
            }
        }
        return text.toString();
    }

    private static List<Integer> cutPoints(AnnotationContent content) {
        List<Integer> points = new ArrayList<>();
        int offset = 0;
        for (int index = 0; index < content.segments().size() - 1; index++) {
            offset += stringOf(content.segments().get(index).get("text")).length();
            points.add(offset);
        }
        return points;
    }

    private static List<Element> elementsOf(AnnotationContent content) {
        List<Element> elements = new ArrayList<>();
        int offset = 0;
        for (int index = 0; index < content.segments().size(); index++) {
            Map<String, Object> segment = content.segments().get(index);
            String text = stringOf(segment.get("text"));
            String type = stringOf(segment.get("type"));
            String id = stringOf(segment.get("id"));
            elements.add(new Element(
                    id.isBlank() ? "P" + (index + 1) : id,
                    null,
                    null,
                    text,
                    type,
                    null,
                    index + 1,
                    offset,
                    offset + text.length(),
                    PROPOSITION_TYPES.contains(type),
                    null,
                    text));
            offset += text.length();
        }
        return elements;
    }

    private static List<Relation> relationsOf(AnnotationContent content) {
        List<Relation> relations = new ArrayList<>();
        for (int index = 0; index < content.relations().size(); index++) {
            Map<String, Object> relation = content.relations().get(index);
            String id = stringOf(relation.get("id"));
            List<String> members = new ArrayList<>();
            if (relation.get("members") instanceof List<?> values) {
                for (Object value : values) {
                    if (value != null) {
                        members.add(value.toString());
                    }
                }
            }
            relations.add(new Relation(
                    id.isBlank() ? "R" + (index + 1) : id,
                    null,
                    id.isBlank() ? "R" + (index + 1) : id,
                    stringOf(relation.get("type")),
                    members,
                    null,
                    stringOf(relation.get("formal")),
                    0));
        }
        return relations;
    }

    private Diagram diagramOf(AnnotationContent content) {
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        for (Map<String, Object> entry : content.diagram()) {
            appendIds(entry.get("nodes"), nodes);
            appendIds(entry.get("relNodes"), nodes);
            if (entry.get("edges") instanceof List<?> values) {
                for (Object value : values) {
                    if (value instanceof Map<?, ?> edge) {
                        edges.add(stringOf(edge.get("from")) + "->" + stringOf(edge.get("to")));
                    }
                }
            }
        }
        return new Diagram(null, null, nodes, edges,
                payloadAdapter.listToJson(content.diagram()), 1, null, null, null);
    }

    private static void appendIds(Object source, List<String> target) {
        if (source instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof Map<?, ?> node && node.get("id") != null) {
                    target.add(node.get("id").toString());
                }
            }
        }
    }

    private static String stringOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String format(LocalDateTime time) {
        return time.format(TIME_FORMAT);
    }

    private record Actor(
            com.seii.legal.common.enums.LoginUser user,
            Account account,
            AnnotationTask task) {
    }

    private record AuthCheck(Actor actor, String error) {
    }
}
