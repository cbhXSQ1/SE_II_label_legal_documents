package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskChangeLog;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.TaskDtos;
import com.seii.legal.rule.guide.GuideRule;
import com.seii.legal.rule.task.TaskRule;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标注任务功能层服务（TASK-01…08）。
 */
@Service
@RequiredArgsConstructor
public class TaskAppService {

    private static final Set<IdentityType> MANAGER_ONLY = Set.of(IdentityType.TASK_MANAGER);
    private static final Set<IdentityType> TASK_VIEW_ROLES = Set.of(
            IdentityType.TASK_MANAGER, IdentityType.ANNOTATOR, IdentityType.ARBITER, IdentityType.ADMIN);

    private final AuthActionTemplate authTemplate;
    private final TaskRepository taskRepository;
    private final AccountRepository accountRepository;
    private final AnnotationRepository annotationRepository;
    private final GuideRepository guideRepository;
    private final TaskRule taskRule;
    private final GuideRule guideRule;
    private final TaskDocumentAdapter documentAdapter;
    private final TaskQueryDispatcher taskQueryDispatcher;
    private final AllowedActionResolver actionResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AppResult<TaskDtos.CreateTaskResponse> create(String credential, TaskDtos.CreateTaskRequest request) {
        return authTemplate.execute(credential, MANAGER_ONLY, null, null, "仅任务管理者可创建任务", ctx -> {
            List<String> annotatorIds = request.annotatorIds() == null ? List.of() : request.annotatorIds();
            if (!taskRule.hasEnoughAnnotators(annotatorIds)) {
                return AppResult.fail("至少需要两名标注员");
            }
            if (!taskRule.hasSingleArbiter(request.arbiterId())) {
                return AppResult.fail("请选择一名仲裁员");
            }
            LocalDateTime now = TimeFormats.now();
            LocalDateTime deadline = TimeFormats.parse(request.deadline());
            TaskStatus initialStatus = request.draft() ? TaskStatus.DRAFT : TaskStatus.PUBLISHED;
            List<String> docIds = documentAdapter.toDocumentIds(request.documentId());

            AnnotationTask draftTask = new AnnotationTask(
                    null,
                    SystemType.ANNOTATION,
                    request.name().trim(),
                    request.description(),
                    deadline,
                    request.guideVersion().trim(),
                    docIds,
                    annotatorIds,
                    request.arbiterId(),
                    initialStatus,
                    request.draft(),
                    ctx.user().accountId(),
                    request.draft() ? null : now,
                    null
            );

            if (!request.draft()) {
                List<String> missing = taskRule.checkPublish(draftTask);
                if (missing != null && !missing.isEmpty()) {
                    return AppResult.fail("任务不满足发布条件：" + String.join("、", missing));
                }
            }

            String taskId = taskRepository.save(draftTask);
            taskRepository.saveAssignment(taskId, annotatorIds, request.arbiterId(), now);
            guideRepository.saveReference("TASK", taskId, request.guideVersion().trim());
            guideRule.registerReference("TASK", taskId, request.guideVersion().trim());

            if (!request.draft()) {
                taskRepository.updateStatus(taskId, TaskStatus.PUBLISHED, ctx.user().accountId(), now);
                publishParticipantsEvent(annotatorIds, request.arbiterId(), "TASK_PUBLISHED",
                        "新标注任务发布", "您被分配了新的标注任务：" + request.name());
            }

            return AppResult.ok(new TaskDtos.CreateTaskResponse(taskId),
                    actionResolver.resolve(initialStatus, IdentityType.TASK_MANAGER));
        });
    }

    @Transactional
    public AppResult<Void> publish(String credential, String taskId) {
        return authTemplate.execute(credential, MANAGER_ONLY, taskId, null, "仅任务管理者可发布任务", ctx -> {
            AnnotationTask task = requireOwnedManagerTask(taskId, ctx.user().accountId());
            List<String> missing = taskRule.checkPublish(task);
            if (missing != null && !missing.isEmpty()) {
                return AppResult.fail("任务不满足发布条件：" + String.join("、", missing));
            }
            TaskStatus next = taskRule.nextStatus(task.status(), TaskRule.TaskAction.PUBLISH);
            taskRepository.updateStatus(taskId, next, ctx.user().accountId(), TimeFormats.now());
            publishParticipantsEvent(task.annotatorIds(), task.arbiterId(), "TASK_PUBLISHED",
                    "新标注任务发布", "任务「" + task.name() + "」已发布，请按时完成。");
            return AppResult.ok(null, actionResolver.resolve(next, IdentityType.TASK_MANAGER));
        });
    }

    @Transactional
    public AppResult<Void> update(String credential, String taskId, TaskDtos.UpdateTaskRequest request) {
        return authTemplate.execute(credential, MANAGER_ONLY, taskId, null, "仅任务管理者可修改任务", ctx -> {
            AnnotationTask existing = requireOwnedManagerTask(taskId, ctx.user().accountId());
            if (existing.status() == TaskStatus.CONFIRMED) {
                return AppResult.fail("任务已确认");
            }
            List<String> newAnnotators = request.annotatorIds() != null ? request.annotatorIds() : existing.annotatorIds();
            String newArbiter = (request.arbiterId() != null && !request.arbiterId().isBlank())
                    ? request.arbiterId() : existing.arbiterId();
            if (!taskRule.hasEnoughAnnotators(newAnnotators)) {
                return AppResult.fail("至少需要两名标注员");
            }
            if (!taskRule.hasSingleArbiter(newArbiter)) {
                return AppResult.fail("请选择一名仲裁员");
            }
            boolean arbiterChanged = existing.arbiterId() != null && !existing.arbiterId().equals(newArbiter);
            if (arbiterChanged && (request.arbiterReason() == null || request.arbiterReason().isBlank())) {
                return AppResult.fail("更换仲裁员请填写变更原因");
            }

            LocalDateTime now = TimeFormats.now();
            LocalDateTime newDeadline = request.deadline() != null ? TimeFormats.parse(request.deadline()) : existing.deadline();
            AnnotationTask updated = new AnnotationTask(
                    existing.taskId(),
                    existing.system(),
                    existing.name(),
                    request.description() != null ? request.description() : existing.description(),
                    newDeadline,
                    existing.guideVersion(),
                    existing.documentIds(),
                    newAnnotators,
                    newArbiter,
                    existing.status(),
                    existing.draftAllowed(),
                    existing.publisherId(),
                    existing.publishedAt(),
                    existing.withdrawReason()
            );

            taskRepository.update(taskId, updated, ctx.user().accountId(), now);
            taskRepository.saveAssignment(taskId, newAnnotators, newArbiter, now);
            taskRepository.saveChangeLog(new TaskChangeLog(
                    null,
                    taskId,
                    "deadline=" + existing.deadline() + ",arbiter=" + existing.arbiterId(),
                    "deadline=" + newDeadline + ",arbiter=" + newArbiter,
                    request.arbiterReason(),
                    ctx.user().accountId(),
                    now
            ));
            publishParticipantsEvent(newAnnotators, newArbiter, "TASK_UPDATED",
                    "标注任务变更通知", "任务「" + existing.name() + "」信息已更新，请及时查看。");
            return AppResult.ok(null, actionResolver.resolve(existing.status(), IdentityType.TASK_MANAGER));
        });
    }

    @Transactional
    public AppResult<Void> withdraw(String credential, String taskId, TaskDtos.WithdrawTaskRequest request) {
        return authTemplate.execute(credential, MANAGER_ONLY, taskId, null, "仅任务管理者可撤回任务", ctx -> {
            AnnotationTask existing = requireOwnedManagerTask(taskId, ctx.user().accountId());
            TaskStatus next = taskRule.nextStatus(existing.status(), TaskRule.TaskAction.WITHDRAW);
            LocalDateTime now = TimeFormats.now();
            AnnotationTask updated = new AnnotationTask(
                    existing.taskId(), existing.system(), existing.name(), existing.description(), existing.deadline(),
                    existing.guideVersion(), existing.documentIds(), existing.annotatorIds(), existing.arbiterId(),
                    next, existing.draftAllowed(), existing.publisherId(), existing.publishedAt(), request.reason()
            );
            taskRepository.update(taskId, updated, ctx.user().accountId(), now);
            taskRepository.updateStatus(taskId, next, ctx.user().accountId(), now);
            publishParticipantsEvent(existing.annotatorIds(), existing.arbiterId(), "TASK_WITHDRAWN",
                    "标注任务撤回通知", "任务「" + existing.name() + "」已撤回，原因：" + request.reason());
            return AppResult.ok(null, actionResolver.resolve(next, IdentityType.TASK_MANAGER));
        });
    }

    @Transactional
    public AppResult<Void> reopen(String credential, String taskId, TaskDtos.ReopenTaskRequest request) {
        return authTemplate.execute(credential, MANAGER_ONLY, taskId, null, "仅任务管理者可重开轮次", ctx -> {
            AnnotationTask existing = requireOwnedManagerTask(taskId, ctx.user().accountId());
            if (existing.status() == TaskStatus.CONFIRMED) {
                return AppResult.fail("任务已确认最终版本，不可重开");
            }
            TaskStatus next = taskRule.nextStatus(existing.status(), TaskRule.TaskAction.REOPEN);
            LocalDateTime now = TimeFormats.now();
            taskRepository.updateStatus(taskId, next, ctx.user().accountId(), now);

            String primaryDoc = documentAdapter.primaryDocumentId(existing.documentIds());
            if (primaryDoc != null) {
                List<Submission> prior = annotationRepository.findSubmissions(taskId, primaryDoc, request.round(), null);
                for (Submission sub : prior) {
                    annotationRepository.updateSubmissionStatus(
                            sub.submissionId(), SubmissionStatus.REOPENED, request.reason(), "轮次重开保留历史");
                }
            }
            publishParticipantsEvent(existing.annotatorIds(), existing.arbiterId(), "TASK_REOPENED",
                    "标注轮次重开通知", "任务「" + existing.name() + "」的 " + request.round() + " 轮已重开，原因：" + request.reason());
            return AppResult.ok(null, actionResolver.resolve(next, IdentityType.TASK_MANAGER));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TaskDtos.MyTasksResponse> myTasks(String credential, Integer page, Integer size) {
        return authTemplate.execute(credential, TASK_VIEW_ROLES, null, null, "当前身份无权查看任务列表", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            PageResult<AnnotationTask> raw = taskQueryDispatcher.dispatch(
                    ctx.user().identity(), ctx.user().accountId(), p, s);
            List<TaskDtos.MyTaskItem> items = raw.records().stream()
                    .map(t -> new TaskDtos.MyTaskItem(
                            t.taskId(),
                            t.name(),
                            t.guideVersion(),
                            TimeFormats.format(t.deadline()),
                            t.status(),
                            t.system() == SystemType.TEACHING,
                            resolveNames(t.annotatorIds()),
                            resolveName(t.arbiterId()),
                            documentAdapter.primaryDocumentId(t.documentIds())
                    ))
                    .toList();
            return AppResult.ok(new TaskDtos.MyTasksResponse(items));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TaskDtos.TaskDetailResponse> detail(String credential, String taskId) {
        return authTemplate.execute(credential, TASK_VIEW_ROLES, taskId, null, "无权查看任务详情", ctx -> {
            AnnotationTask task = requireVisibleTask(taskId, ctx.user().accountId(), ctx.user().identity());
            TaskDtos.TaskDetailResponse dto = new TaskDtos.TaskDetailResponse(
                    task.taskId(),
                    task.name(),
                    task.description(),
                    TimeFormats.format(task.deadline()),
                    task.guideVersion(),
                    documentAdapter.primaryDocumentId(task.documentIds()),
                    resolveNames(task.annotatorIds()),
                    resolveName(task.arbiterId()),
                    task.status(),
                    resolveName(task.publisherId()),
                    TimeFormats.format(task.publishedAt()),
                    task.withdrawReason()
            );
            return AppResult.ok(dto, actionResolver.resolve(task.status(), ctx.user().identity()));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TaskDtos.TaskProgressResponse> progress(String credential, String taskId) {
        return authTemplate.execute(credential, TASK_VIEW_ROLES, taskId, null, "无权查看任务进度", ctx -> {
            AnnotationTask task = requireVisibleTask(taskId, ctx.user().accountId(), ctx.user().identity());
            TaskProgress progress = taskRepository.findProgress(taskId);
            String docId = documentAdapter.primaryDocumentId(task.documentIds());
            RoundType currentRound = (task.status() == TaskStatus.CUTTING_CONFIRMED
                    || task.status() == TaskStatus.STRUCTURED_PENDING
                    || task.status() == TaskStatus.CONFIRMED)
                    ? RoundType.STRUCTURED : RoundType.CUTTING;
            List<Submission> submissions = docId == null
                    ? List.of()
                    : annotationRepository.findSubmissions(taskId, docId, currentRound, null);
            Map<String, Submission> latestByAnnotator = submissions.stream()
                    .collect(Collectors.toMap(Submission::annotatorId, s -> s, (a, b) -> b));

            List<String> annotatorIds = task.annotatorIds() == null ? List.of() : task.annotatorIds();
            List<TaskDtos.AnnotatorProgressItem> annotators = annotatorIds.stream()
                    .map(id -> {
                        Submission sub = latestByAnnotator.get(id);
                        return new TaskDtos.AnnotatorProgressItem(
                                id,
                                resolveName(id),
                                sub == null ? "" : TimeFormats.format(sub.submittedAt()),
                                sub == null ? "未提交" : sub.status().name()
                        );
                    })
                    .toList();

            int due = progress != null ? progress.due() : annotatorIds.size();
            int submitted = progress != null ? progress.submitted() : latestByAnnotator.size();
            int unsubmitted = progress != null ? progress.unsubmitted() : Math.max(0, due - submitted);
            int conflicts = progress != null ? progress.conflictCount() : 0;
            int confirmed = progress != null ? progress.confirmedCount() : 0;
            return AppResult.ok(new TaskDtos.TaskProgressResponse(due, submitted, unsubmitted, conflicts, confirmed, annotators));
        });
    }

    private AnnotationTask requireOwnedManagerTask(String taskId, String managerId) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!managerId.equals(task.publisherId())) {
            throw new BusinessException("仅任务创建者可执行该操作");
        }
        return task;
    }

    private AnnotationTask requireVisibleTask(String taskId, String accountId, IdentityType identity) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (identity == IdentityType.ADMIN) {
            return task;
        }
        if (identity == IdentityType.TASK_MANAGER && accountId.equals(task.publisherId())) {
            return task;
        }
        if (identity == IdentityType.ARBITER && accountId.equals(task.arbiterId())) {
            return task;
        }
        if (identity == IdentityType.ANNOTATOR && task.annotatorIds() != null && task.annotatorIds().contains(accountId)) {
            return task;
        }
        throw new BusinessException("无权访问该任务");
    }

    private void publishParticipantsEvent(List<String> annotatorIds, String arbiterId, String type, String subject, String content) {
        List<String> receivers = new ArrayList<>();
        if (annotatorIds != null) {
            receivers.addAll(annotatorIds);
        }
        if (arbiterId != null && !arbiterId.isBlank()) {
            receivers.add(arbiterId);
        }
        eventPublisher.publishEvent(new DomainNotificationEvent(receivers, type, subject, content, false));
    }

    private List<String> resolveNames(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(this::resolveName).toList();
    }

    private String resolveName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }
}
