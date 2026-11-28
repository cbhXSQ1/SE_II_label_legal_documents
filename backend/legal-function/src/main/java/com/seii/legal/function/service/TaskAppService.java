package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskAssignment;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.task.AnnotatorProgress;
import com.seii.legal.function.dto.task.CreateTaskCommand;
import com.seii.legal.function.dto.task.ReopenTaskCommand;
import com.seii.legal.function.dto.task.TaskDetailView;
import com.seii.legal.function.dto.task.TaskIdView;
import com.seii.legal.function.dto.task.TaskListItem;
import com.seii.legal.function.dto.task.TaskListView;
import com.seii.legal.function.dto.task.TaskProgressView;
import com.seii.legal.function.dto.task.UpdateTaskCommand;
import com.seii.legal.function.dto.task.WithdrawTaskCommand;
import com.seii.legal.function.support.AuthActionTemplate;
import com.seii.legal.function.support.TaskDocumentAdapter;
import com.seii.legal.function.support.TaskQueryDispatcher;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskAppService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TASK_REFERENCE = "TASK";
    private static final String ROUND_OPEN = "OPEN";
    private static final String ONLY_MANAGER = "只有任务管理者可以执行该操作";
    private static final String TASK_MISSING = "任务不存在";
    private static final String NO_ACCESS = "无权查看该任务";
    private static final String CONFIRMED = "任务已确认";
    private static final String NO_IDENTITY = "没有可进入的身份，请重新选择";
    private static final String NO_ROLE = "账号没有该身份";
    private static final String SUBMITTED = "已提交";
    private static final String NOT_SUBMITTED = "未提交";

    private final AuthActionTemplate authTemplate;
    private final AccountRepository accountRepository;
    private final IdentityRule identityRule;
    private final TaskRepository taskRepository;
    private final AnnotationRepository annotationRepository;
    private final GuideRepository guideRepository;
    private final TaskRule taskRule;
    private final TaskDocumentAdapter documentAdapter;
    private final TaskQueryDispatcher queryDispatcher;

    public TaskAppService(AuthActionTemplate authTemplate, AccountRepository accountRepository,
                          IdentityRule identityRule, TaskRepository taskRepository,
                          AnnotationRepository annotationRepository, GuideRepository guideRepository,
                          TaskRule taskRule, TaskDocumentAdapter documentAdapter,
                          TaskQueryDispatcher queryDispatcher) {
        this.authTemplate = authTemplate;
        this.accountRepository = accountRepository;
        this.identityRule = identityRule;
        this.taskRepository = taskRepository;
        this.annotationRepository = annotationRepository;
        this.guideRepository = guideRepository;
        this.taskRule = taskRule;
        this.documentAdapter = documentAdapter;
        this.queryDispatcher = queryDispatcher;
    }

    @Transactional
    public AppResult<TaskIdView> create(String credential, CreateTaskCommand command) {
        return authTemplate.execute(credential, Set.of(IdentityType.TASK_MANAGER), null, null,
                ONLY_MANAGER, ctx -> {
                    LoginUser user = ctx.user();
                    List<String> annotatorIds = safeList(command.annotatorIds());
                    if (!taskRule.hasEnoughAnnotators(annotatorIds)) {
                        return AppResult.fail("至少需要两名标注员");
                    }
                    if (!taskRule.hasSingleArbiter(command.arbiterId())) {
                        return AppResult.fail("请选择一名仲裁员");
                    }
                    LocalDateTime deadline = parseTime(command.deadline());
                    List<String> documentIds = documentAdapter.toDocumentIds(command.documentId());
                    SystemType system = user.system() != null ? user.system() : SystemType.ANNOTATION;

                    AnnotationTask draft = new AnnotationTask(null, system, command.name(),
                            command.description(), deadline, command.guideVersion(), documentIds,
                            annotatorIds, command.arbiterId(), TaskStatus.DRAFT, true,
                            user.accountId(), null, null);
                    List<String> missing = taskRule.checkPublish(draft);
                    if (missing != null && !missing.isEmpty()) {
                        return AppResult.fail("缺少：" + String.join("、", missing));
                    }

                    TaskStatus status = TaskStatus.DRAFT;
                    LocalDateTime publishedAt = null;
                    if (!command.draft()) {
                        TaskStatus next = taskRule.nextStatus(
                                TaskStatus.DRAFT, TaskRule.TaskAction.PUBLISH);
                        status = next != null ? next : TaskStatus.PUBLISHED;
                        publishedAt = LocalDateTime.now();
                    }
                    AnnotationTask task = new AnnotationTask(null, system, command.name(),
                            command.description(), deadline, command.guideVersion(), documentIds,
                            annotatorIds, command.arbiterId(), status, true, user.accountId(),
                            publishedAt, null);
                    String taskId = taskRepository.save(task);
                    LocalDateTime now = LocalDateTime.now();
                    taskRepository.saveAssignment(taskId, annotatorIds, command.arbiterId(), now);
                    guideRepository.saveReference(TASK_REFERENCE, taskId, command.guideVersion());
                    if (!documentIds.isEmpty()) {
                        taskRepository.saveRound(new TaskRound(null, taskId, documentIds.get(0),
                                RoundType.CUTTING, 1, ROUND_OPEN, now, null));
                    }
                    return AppResult.ok(new TaskIdView(taskId));
                });
    }

    @Transactional
    public AppResult<Void> publish(String credential, String taskId) {
        return authTemplate.execute(credential, Set.of(IdentityType.TASK_MANAGER), taskId, null,
                ONLY_MANAGER, ctx -> {
                    AnnotationTask task = taskRepository.findById(taskId).orElse(null);
                    if (task == null) {
                        return AppResult.fail(TASK_MISSING);
                    }
                    if (!ctx.account().accountId().equals(task.publisherId())) {
                        return AppResult.fail(ONLY_MANAGER);
                    }
                    if (task.status() == TaskStatus.CONFIRMED) {
                        return AppResult.fail(CONFIRMED);
                    }
                    if (task.status() == TaskStatus.WITHDRAWN) {
                        return AppResult.fail("任务已撤回");
                    }
                    TaskStatus next = taskRule.nextStatus(
                            task.status(), TaskRule.TaskAction.PUBLISH);
                    if (next == null) {
                        return AppResult.fail("当前状态不能发布");
                    }
                    taskRepository.updateStatus(taskId, next, ctx.user().accountId(),
                            LocalDateTime.now());
                    return AppResult.ok(null);
                });
    }

    @Transactional
    public AppResult<Void> update(String credential, String taskId, UpdateTaskCommand command) {
        return authTemplate.execute(credential, Set.of(IdentityType.TASK_MANAGER), taskId, null,
                ONLY_MANAGER, ctx -> {
                    AnnotationTask task = taskRepository.findById(taskId).orElse(null);
                    if (task == null) {
                        return AppResult.fail(TASK_MISSING);
                    }
                    if (!ctx.account().accountId().equals(task.publisherId())) {
                        return AppResult.fail(ONLY_MANAGER);
                    }
                    if (task.status() == TaskStatus.CONFIRMED) {
                        return AppResult.fail(CONFIRMED);
                    }
                    if (task.status() == TaskStatus.WITHDRAWN) {
                        return AppResult.fail("任务已撤回");
                    }
                    TaskStatus next = taskRule.nextStatus(
                            task.status(), TaskRule.TaskAction.UPDATE);
                    if (next == null) {
                        return AppResult.fail("当前状态不能修改");
                    }
                    String description = command.description() != null
                            ? command.description() : task.description();
                    LocalDateTime deadline = command.deadline() != null
                            ? parseTime(command.deadline()) : task.deadline();
                    List<String> annotatorIds = command.annotatorIds() != null
                            ? safeList(command.annotatorIds()) : task.annotatorIds();
                    String arbiterId = command.arbiterId() != null
                            ? command.arbiterId() : task.arbiterId();
                    AnnotationTask changes = new AnnotationTask(task.taskId(), task.system(),
                            task.name(), description, deadline, task.guideVersion(),
                            task.documentIds(), annotatorIds, arbiterId, next, task.draftAllowed(),
                            task.publisherId(), task.publishedAt(), task.withdrawReason());
                    LocalDateTime now = LocalDateTime.now();
                    taskRepository.update(taskId, changes, ctx.user().accountId(), now);
                    if (command.annotatorIds() != null || command.arbiterId() != null) {
                        taskRepository.saveAssignment(taskId, annotatorIds, arbiterId, now);
                    }
                    return AppResult.ok(null);
                });
    }

    @Transactional
    public AppResult<Void> withdraw(String credential, String taskId, WithdrawTaskCommand command) {
        return authTemplate.execute(credential, Set.of(IdentityType.TASK_MANAGER), taskId, null,
                ONLY_MANAGER, ctx -> {
                    AnnotationTask task = taskRepository.findById(taskId).orElse(null);
                    if (task == null) {
                        return AppResult.fail(TASK_MISSING);
                    }
                    if (!ctx.account().accountId().equals(task.publisherId())) {
                        return AppResult.fail(ONLY_MANAGER);
                    }
                    if (task.status() == TaskStatus.CONFIRMED) {
                        return AppResult.fail(CONFIRMED);
                    }
                    TaskStatus next = taskRule.nextStatus(
                            task.status(), TaskRule.TaskAction.WITHDRAW);
                    if (next == null) {
                        return AppResult.fail("当前状态不能撤回");
                    }
                    String reason = command != null ? command.reason() : null;
                    AnnotationTask changes = new AnnotationTask(task.taskId(), task.system(),
                            task.name(), task.description(), task.deadline(),
                            task.guideVersion(), task.documentIds(), task.annotatorIds(),
                            task.arbiterId(), next, task.draftAllowed(), task.publisherId(),
                            task.publishedAt(), reason);
                    LocalDateTime now = LocalDateTime.now();
                    taskRepository.update(taskId, changes, ctx.user().accountId(), now);
                    taskRepository.updateStatus(taskId, next, ctx.user().accountId(), now);
                    return AppResult.ok(null);
                });
    }

    @Transactional
    public AppResult<Void> reopen(String credential, String taskId, ReopenTaskCommand command) {
        return authTemplate.execute(credential, Set.of(IdentityType.TASK_MANAGER), taskId, null,
                ONLY_MANAGER, ctx -> {
                    AnnotationTask task = taskRepository.findById(taskId).orElse(null);
                    if (task == null) {
                        return AppResult.fail(TASK_MISSING);
                    }
                    if (!ctx.account().accountId().equals(task.publisherId())) {
                        return AppResult.fail(ONLY_MANAGER);
                    }
                    if (task.status() == TaskStatus.CONFIRMED) {
                        return AppResult.fail(CONFIRMED);
                    }
                    TaskStatus next = taskRule.nextStatus(
                            task.status(), TaskRule.TaskAction.REOPEN);
                    if (next == null) {
                        return AppResult.fail("当前状态不能重开");
                    }
                    String documentId = documentAdapter.toDocumentId(task.documentIds());
                    LocalDateTime now = LocalDateTime.now();
                    TaskRound oldRound = selectRound(taskId, documentId,
                            command != null ? command.round() : null);
                    int sequence = 1;
                    RoundType type = RoundType.CUTTING;
                    if (oldRound != null) {
                        taskRepository.closeRound(oldRound.roundId(), now);
                        sequence = oldRound.sequence() + 1;
                        type = oldRound.type();
                    }
                    taskRepository.saveRound(new TaskRound(null, taskId, documentId, type,
                            sequence, ROUND_OPEN, now, null));
                    taskRepository.updateStatus(taskId, next, ctx.user().accountId(), now);
                    return AppResult.ok(null);
                });
    }

    @Transactional(readOnly = true)
    public AppResult<TaskListView> myTasks(String credential, SystemType system, int page, int size) {
        return authTemplate.execute(credential, null, null, null, null, ctx -> {
            LoginUser user = ctx.user();
            IdentityType identity = user.identity();
            if (identity == null) {
                return AppResult.fail(NO_IDENTITY);
            }
            if (!identityRule.hasRole(ctx.account(), identity)) {
                return AppResult.fail(NO_ROLE);
            }
            SystemType targetSystem = system != null
                    ? system
                    : (user.system() != null ? user.system() : SystemType.ANNOTATION);
            PageResult<AnnotationTask> result = queryDispatcher.dispatch(
                    identity, user.accountId(), targetSystem,
                    normalizePage(page), normalizeSize(size));
            List<TaskListItem> items = result.records().stream().map(this::toListItem).toList();
            return AppResult.ok(new TaskListView(items));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TaskDetailView> detail(String credential, String taskId) {
        return authTemplate.execute(credential, null, taskId, null, null, ctx -> {
            AnnotationTask task = taskRepository.findById(taskId).orElse(null);
            if (task == null) {
                return AppResult.fail(TASK_MISSING);
            }
            if (!canView(task, ctx.account())) {
                return AppResult.fail(NO_ACCESS);
            }
            TaskAssignment assignment = taskRepository.findAssignment(taskId).orElse(null);
            List<String> annotators = displayNames(assignment != null
                    ? assignment.annotatorIds() : task.annotatorIds());
            String arbiter = displayName(assignment != null
                    ? assignment.arbiterId() : task.arbiterId());
            String publisher = accountRepository.findById(task.publisherId())
                    .map(Account::name).orElse(task.publisherId());
            TaskDetailView view = new TaskDetailView(task.taskId(), task.name(),
                    task.description(), formatTime(task.deadline()), task.guideVersion(),
                    documentAdapter.toDocumentId(task.documentIds()), annotators, arbiter,
                    task.status(), publisher, formatTime(task.publishedAt()),
                    task.withdrawReason());
            return AppResult.ok(view);
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TaskProgressView> progress(String credential, String taskId) {
        return authTemplate.execute(credential, null, taskId, null, null, ctx -> {
            AnnotationTask task = taskRepository.findById(taskId).orElse(null);
            if (task == null) {
                return AppResult.fail(TASK_MISSING);
            }
            if (!canView(task, ctx.account())) {
                return AppResult.fail(NO_ACCESS);
            }
            TaskProgress progress = taskRepository.findProgress(taskId);
            TaskAssignment assignment = taskRepository.findAssignment(taskId).orElse(null);
            List<String> annotatorIds = assignment != null
                    ? assignment.annotatorIds() : task.annotatorIds();
            List<Submission> submissions = progressRound(task)
                    .map(round -> annotationRepository.findSubmissionsByRound(
                            round.roundId(), null))
                    .orElseGet(List::of);
            Map<String, Submission> latest = latestSubmissions(submissions);
            List<AnnotatorProgress> annotators = new ArrayList<>();
            if (annotatorIds != null) {
                for (String annotatorId : annotatorIds) {
                    String name = accountRepository.findById(annotatorId)
                            .map(Account::name).orElse(annotatorId);
                    Submission submission = latest.get(annotatorId);
                    annotators.add(new AnnotatorProgress(annotatorId, name,
                            submission != null ? formatTime(submission.submittedAt()) : null,
                            submission != null ? SUBMITTED : NOT_SUBMITTED));
                }
            }
            TaskProgressView view = new TaskProgressView(progress.expectedCount(),
                    progress.submittedCount(), progress.unsubmittedCount(),
                    progress.conflictCount(), progress.confirmedCount(), annotators);
            return AppResult.ok(view);
        });
    }

    private Optional<TaskRound> currentRound(AnnotationTask task) {
        String documentId = documentAdapter.toDocumentId(task.documentIds());
        if (documentId == null) {
            return Optional.empty();
        }
        for (RoundType type : roundOrder(task.status())) {
            Optional<TaskRound> round = taskRepository.findCurrentRound(
                    task.taskId(), documentId, type);
            if (round.isPresent()) {
                return round;
            }
        }
        return Optional.empty();
    }

    private Optional<TaskRound> progressRound(AnnotationTask task) {
        Optional<TaskRound> current = currentRound(task);
        if (current.isPresent()) {
            return current;
        }
        String documentId = documentAdapter.toDocumentId(task.documentIds());
        if (documentId == null) {
            return Optional.empty();
        }
        TaskRound latest = null;
        for (TaskRound item : taskRepository.findRounds(task.taskId(), documentId)) {
            if (latest == null || item.sequence() > latest.sequence()) {
                latest = item;
            }
        }
        return Optional.ofNullable(latest);
    }

    private static List<RoundType> roundOrder(TaskStatus status) {
        if (status != null
                && (status == TaskStatus.CUTTING_CONFIRMED
                || status == TaskStatus.STRUCTURED_PENDING
                || status == TaskStatus.CONFIRMED)) {
            return List.of(RoundType.STRUCTURED, RoundType.CUTTING);
        }
        return List.of(RoundType.CUTTING, RoundType.STRUCTURED);
    }

    private TaskRound selectRound(String taskId, String documentId, Integer round) {
        List<TaskRound> rounds = taskRepository.findRounds(taskId, documentId);
        if (round != null) {
            for (TaskRound item : rounds) {
                if (item.sequence() == round) {
                    return item;
                }
            }
        }
        TaskRound current = null;
        for (TaskRound item : rounds) {
            if (current == null || item.sequence() > current.sequence()) {
                current = item;
            }
        }
        return current;
    }

    private Map<String, Submission> latestSubmissions(List<Submission> submissions) {
        Map<String, Submission> latest = new HashMap<>();
        for (Submission submission : submissions) {
            Submission existing = latest.get(submission.annotatorId());
            if (existing == null || isAfter(submission.submittedAt(), existing.submittedAt())) {
                latest.put(submission.annotatorId(), submission);
            }
        }
        return latest;
    }

    private static boolean isAfter(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return false;
        }
        return right == null || left.isAfter(right);
    }

    private boolean canView(AnnotationTask task, Account account) {
        if (identityRule.hasRole(account, IdentityType.ADMIN)) {
            return true;
        }
        if (account.accountId().equals(task.publisherId())) {
            return true;
        }
        if (task.annotatorIds() != null && task.annotatorIds().contains(account.accountId())) {
            return true;
        }
        return account.accountId().equals(task.arbiterId());
    }

    private TaskListItem toListItem(AnnotationTask task) {
        return new TaskListItem(task.taskId(), task.name(), task.guideVersion(),
                formatTime(task.deadline()), task.status(), task.system() == SystemType.TEACHING,
                displayNames(task.annotatorIds()), displayName(task.arbiterId()),
                documentAdapter.toDocumentId(task.documentIds()));
    }

    private List<String> displayNames(List<String> accountIds) {
        if (accountIds == null) {
            return List.of();
        }
        return accountIds.stream().map(this::displayName).toList();
    }

    private String displayName(String accountId) {
        if (accountId == null) {
            return null;
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException first) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException second) {
                throw new IllegalStateException("时间格式不正确");
            }
        }
    }

    private static String formatTime(LocalDateTime time) {
        return time != null ? time.format(TIME_FORMAT) : null;
    }

    private static List<String> safeList(List<String> values) {
        return values != null ? values : List.of();
    }

    private static int normalizePage(int page) {
        return page > 0 ? page : 1;
    }

    private static int normalizeSize(int size) {
        return size > 0 ? size : 20;
    }
}
