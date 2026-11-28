package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.Draft;
import com.seii.legal.data.entity.Element;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.ReadingRecord;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskAssignment;
import com.seii.legal.data.entity.TaskChangeLog;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.task.AnnotatorProgress;
import com.seii.legal.function.dto.task.CreateTaskCommand;
import com.seii.legal.function.dto.task.ReopenTaskCommand;
import com.seii.legal.function.dto.task.TaskDetailView;
import com.seii.legal.function.dto.task.TaskIdView;
import com.seii.legal.function.dto.task.TaskListView;
import com.seii.legal.function.dto.task.TaskProgressView;
import com.seii.legal.function.dto.task.UpdateTaskCommand;
import com.seii.legal.function.dto.task.WithdrawTaskCommand;
import com.seii.legal.function.support.AdminTaskQueryStrategy;
import com.seii.legal.function.support.AnnotatorTaskQueryStrategy;
import com.seii.legal.function.support.ArbiterTaskQueryStrategy;
import com.seii.legal.function.support.ManagerTaskQueryStrategy;
import com.seii.legal.function.support.TaskDocumentAdapter;
import com.seii.legal.function.support.TaskQueryDispatcher;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskAppServiceTest {

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository(List.of(
            account("mgr", "manager", "李管理", IdentityType.TASK_MANAGER),
            account("a1", "annot1", "张标注", IdentityType.ANNOTATOR),
            account("a2", "annot2", "王标注", IdentityType.ANNOTATOR),
            account("arb", "arbiter", "赵仲裁", IdentityType.ARBITER),
            account("root", "admin", "钱管理", IdentityType.ADMIN)));
    private final FakeIdentityRule identities = new FakeIdentityRule();
    private final FakeTaskRepository tasks = new FakeTaskRepository();
    private final FakeAnnotationRepository annotations = new FakeAnnotationRepository();
    private final FakeGuideRepository guides = new FakeGuideRepository();
    private final FakeTaskRule rules = new FakeTaskRule();
    private final TaskQueryDispatcher dispatcher = new TaskQueryDispatcher(List.of(
            new ManagerTaskQueryStrategy(tasks),
            new AnnotatorTaskQueryStrategy(tasks),
            new ArbiterTaskQueryStrategy(tasks),
            new AdminTaskQueryStrategy(tasks)));
    private final TaskAppService service = new TaskAppService(
            sessions, accounts, identities, tasks, annotations, guides, rules,
            new TaskDocumentAdapter(), dispatcher);

    TaskAppServiceTest() {
        sessions.saved.put("cred-mgr", new LoginUser("mgr", SystemType.ANNOTATION,
                IdentityType.TASK_MANAGER, "cred-mgr", LocalDateTime.now().plusHours(1)));
        sessions.saved.put("cred-a1", new LoginUser("a1", SystemType.ANNOTATION,
                IdentityType.ANNOTATOR, "cred-a1", LocalDateTime.now().plusHours(1)));
        sessions.saved.put("cred-arb", new LoginUser("arb", SystemType.ANNOTATION,
                IdentityType.ARBITER, "cred-arb", LocalDateTime.now().plusHours(1)));
        sessions.saved.put("cred-root", new LoginUser("root", SystemType.ANNOTATION,
                IdentityType.ADMIN, "cred-root", LocalDateTime.now().plusHours(1)));
        sessions.saved.put("cred-noid", new LoginUser("mgr", SystemType.ANNOTATION,
                null, "cred-noid", LocalDateTime.now().plusHours(1)));
    }

    @Test
    void createSavesTaskAssignmentGuideReferenceAndFirstRound() {
        AppResult<TaskIdView> result = service.create("cred-mgr", createCommand(true));

        assertTrue(result.success());
        String taskId = result.data().taskId();
        AnnotationTask saved = tasks.tasks.get(taskId);
        assertNotNull(saved);
        assertEquals("合同标注", saved.name());
        assertEquals(SystemType.ANNOTATION, saved.system());
        assertEquals(TaskStatus.DRAFT, saved.status());
        assertEquals(List.of("d1"), saved.documentIds());
        assertEquals(List.of("a1", "a2"), saved.annotatorIds());
        assertEquals("arb", saved.arbiterId());
        assertEquals("mgr", saved.publisherId());
        assertTrue(saved.draftAllowed());

        assertEquals("arb", tasks.assignments.get(taskId).arbiterId());
        assertEquals(List.of("a1", "a2"), tasks.assignments.get(taskId).annotatorIds());
        assertEquals("v1.2", guides.references.get("TASK|" + taskId));

        assertEquals(1, tasks.rounds.size());
        TaskRound round = tasks.rounds.values().iterator().next();
        assertEquals(taskId, round.taskId());
        assertEquals("d1", round.documentId());
        assertEquals(RoundType.CUTTING, round.type());
        assertEquals(1, round.sequence());
        assertEquals("OPEN", round.status());
        assertNotNull(round.openedAt());
    }

    @Test
    void createWithoutDraftPublishesTask() {
        AppResult<TaskIdView> result = service.create("cred-mgr", createCommand(false));

        assertTrue(result.success());
        AnnotationTask saved = tasks.tasks.get(result.data().taskId());
        assertEquals(TaskStatus.PUBLISHED, saved.status());
        assertEquals(TaskRule.TaskAction.PUBLISH, rules.lastAction);
        assertNotNull(saved.publishedAt());
    }

    @Test
    void createRejectsInsufficientAnnotators() {
        AppResult<TaskIdView> result = service.create("cred-mgr", new CreateTaskCommand(
                "合同标注", "说明", "2030-01-01 10:00:00", "v1.2", "d1",
                List.of("a1"), "arb", true));

        assertFalse(result.success());
        assertEquals("至少需要两名标注员", result.message());
        assertTrue(tasks.tasks.isEmpty());
    }

    @Test
    void createRejectsMissingArbiter() {
        AppResult<TaskIdView> result = service.create("cred-mgr", new CreateTaskCommand(
                "合同标注", "说明", "2030-01-01 10:00:00", "v1.2", "d1",
                List.of("a1", "a2"), "", true));

        assertFalse(result.success());
        assertEquals("请选择一名仲裁员", result.message());
        assertTrue(tasks.tasks.isEmpty());
    }

    @Test
    void createRejectsWhenPublishCheckReportsMissingFields() {
        rules.missing = List.of("指南版本", "文书");

        AppResult<TaskIdView> result = service.create("cred-mgr", createCommand(true));

        assertFalse(result.success());
        assertTrue(result.message().contains("指南版本"));
        assertTrue(result.message().contains("文书"));
        assertTrue(tasks.tasks.isEmpty());
    }

    @Test
    void publishMovesDraftToPublished() {
        tasks.put(task("t1", TaskStatus.DRAFT));

        AppResult<Void> result = service.publish("cred-mgr", "t1");

        assertTrue(result.success());
        assertEquals(TaskStatus.PUBLISHED, tasks.tasks.get("t1").status());
        assertEquals(TaskStatus.DRAFT, rules.lastCurrent);
        assertEquals(TaskRule.TaskAction.PUBLISH, rules.lastAction);
        assertEquals(TaskStatus.PUBLISHED, tasks.lastStatusUpdate);
    }

    @Test
    void publishRejectsNonManager() {
        tasks.put(task("t1", TaskStatus.DRAFT));

        AppResult<Void> result = service.publish("cred-a1", "t1");

        assertFalse(result.success());
        assertEquals("只有任务管理者可以执行该操作", result.message());
        assertEquals(TaskStatus.DRAFT, tasks.tasks.get("t1").status());
    }

    @Test
    void updateChangesFieldsAndAssignment() {
        tasks.put(task("t1", TaskStatus.PUBLISHED));

        AppResult<Void> result = service.update("cred-mgr", "t1", new UpdateTaskCommand(
                "新说明", "2031-05-01 09:00:00", List.of("a1", "a2", "a3"), "arb2"));

        assertTrue(result.success());
        AnnotationTask updated = tasks.tasks.get("t1");
        assertEquals("新说明", updated.description());
        assertEquals(LocalDateTime.of(2031, 5, 1, 9, 0, 0), updated.deadline());
        assertEquals(List.of("a1", "a2", "a3"), updated.annotatorIds());
        assertEquals("arb2", updated.arbiterId());
        assertEquals(List.of("a1", "a2", "a3"), tasks.assignments.get("t1").annotatorIds());
        assertEquals("arb2", tasks.assignments.get("t1").arbiterId());
        assertEquals(TaskRule.TaskAction.UPDATE, rules.lastAction);
    }

    @Test
    void updateRejectsConfirmedTask() {
        tasks.put(task("t1", TaskStatus.CONFIRMED));

        AppResult<Void> result = service.update("cred-mgr", "t1", new UpdateTaskCommand("x", null, null, null));

        assertFalse(result.success());
        assertEquals("任务已确认", result.message());
    }

    @Test
    void withdrawStoresReasonAndStatus() {
        tasks.put(task("t1", TaskStatus.PUBLISHED));

        AppResult<Void> result = service.withdraw("cred-mgr", "t1", new WithdrawTaskCommand("重复任务"));

        assertTrue(result.success());
        assertEquals(TaskStatus.WITHDRAWN, tasks.tasks.get("t1").status());
        assertEquals("重复任务", tasks.tasks.get("t1").withdrawReason());
        assertEquals(TaskRule.TaskAction.WITHDRAW, rules.lastAction);
    }

    @Test
    void withdrawRejectsConfirmedTask() {
        tasks.put(task("t1", TaskStatus.CONFIRMED));

        AppResult<Void> result = service.withdraw("cred-mgr", "t1", new WithdrawTaskCommand("撤销"));

        assertFalse(result.success());
        assertEquals("任务已确认", result.message());
        assertEquals(TaskStatus.CONFIRMED, tasks.tasks.get("t1").status());
    }

    @Test
    void reopenClosesOldRoundAndOpensNextRound() {
        tasks.put(task("t1", TaskStatus.CUTTING_PENDING));
        tasks.rounds.put("r1", new TaskRound("r1", "t1", "d1", RoundType.CUTTING, 1, "LOCKED",
                LocalDateTime.of(2026, 1, 1, 9, 0), null));

        AppResult<Void> result = service.reopen("cred-mgr", "t1", new ReopenTaskCommand(1, "重新标注"));

        assertTrue(result.success());
        assertEquals(TaskStatus.REOPENED, tasks.tasks.get("t1").status());
        assertTrue(tasks.closedRounds.contains("r1"));
        assertNotNull(tasks.rounds.get("r1").closedAt());
        assertEquals(2, tasks.rounds.size());
        TaskRound reopened = tasks.rounds.values().stream()
                .filter(r -> !r.roundId().equals("r1"))
                .findFirst().orElseThrow();
        assertEquals(RoundType.CUTTING, reopened.type());
        assertEquals(2, reopened.sequence());
        assertEquals(task("t1", TaskStatus.REOPENED).taskId(), reopened.taskId());
        assertEquals(TaskRule.TaskAction.REOPEN, rules.lastAction);
    }

    @Test
    void reopenRejectsConfirmedTask() {
        tasks.put(task("t1", TaskStatus.CONFIRMED));

        AppResult<Void> result = service.reopen("cred-mgr", "t1", new ReopenTaskCommand(1, "重开"));

        assertFalse(result.success());
        assertEquals("任务已确认", result.message());
    }

    @Test
    void myTasksDispatchesByManagerIdentity() {
        tasks.put(task("t1", TaskStatus.PUBLISHED));
        tasks.put(new AnnotationTask("t2", SystemType.ANNOTATION, "他人任务", "", null, "v1",
                List.of("d2"), List.of("a1"), "arb", TaskStatus.DRAFT, true, "other", null, null));

        AppResult<TaskListView> result = service.myTasks("cred-mgr", SystemType.ANNOTATION, 1, 20);

        assertTrue(result.success());
        assertEquals(1, result.data().tasks().size());
        assertEquals("t1", result.data().tasks().get(0).id());
        assertEquals("v1.2", result.data().tasks().get(0).guide());
        assertEquals(List.of("张标注", "王标注"), result.data().tasks().get(0).annotators());
        assertEquals("赵仲裁", result.data().tasks().get(0).arbiter());
        assertTrue(tasks.managerQueried);
        assertFalse(tasks.annotatorQueried);
    }

    @Test
    void myTasksDispatchesByAnnotatorIdentity() {
        tasks.put(task("t1", TaskStatus.PUBLISHED));

        AppResult<TaskListView> result = service.myTasks("cred-a1", SystemType.ANNOTATION, 1, 20);

        assertTrue(result.success());
        assertEquals(1, result.data().tasks().size());
        assertTrue(tasks.annotatorQueried);
    }

    @Test
    void myTasksDispatchesAdminToSystemWideQuery() {
        tasks.put(task("t1", TaskStatus.PUBLISHED));

        AppResult<TaskListView> result = service.myTasks("cred-root", SystemType.ANNOTATION, 1, 20);

        assertTrue(result.success());
        assertEquals(1, result.data().tasks().size());
        assertTrue(tasks.findAllQueried);
        assertEquals(SystemType.ANNOTATION, tasks.lastFindAllSystem);
    }

    @Test
    void myTasksRejectsSessionWithoutIdentity() {
        AppResult<TaskListView> result = service.myTasks("cred-noid", SystemType.ANNOTATION, 1, 20);

        assertFalse(result.success());
        assertEquals("没有可进入的身份，请重新选择", result.message());
    }

    @Test
    void detailMapsDocumentAndAssignment() {
        tasks.put(task("t1", TaskStatus.PUBLISHED));
        tasks.assignments.put("t1", new TaskAssignment("t1", List.of("a1", "a2"), "arb",
                LocalDateTime.of(2026, 1, 1, 9, 0)));

        AppResult<TaskDetailView> result = service.detail("cred-arb", "t1");

        assertTrue(result.success());
        TaskDetailView view = result.data();
        assertEquals("t1", view.id());
        assertEquals("合同标注", view.name());
        assertEquals("d1", view.documentId());
        assertEquals(List.of("张标注", "王标注"), view.annotators());
        assertEquals("赵仲裁", view.arbiter());
        assertEquals(TaskStatus.PUBLISHED, view.status());
        assertEquals("李管理", view.publisher());
        assertEquals("2030-01-01 10:00:00", view.deadline());
    }

    @Test
    void detailRejectsUnrelatedAnnotator() {
        tasks.put(new AnnotationTask("t1", SystemType.ANNOTATION, "合同标注", "说明",
                LocalDateTime.of(2030, 1, 1, 10, 0), "v1.2", List.of("d1"),
                List.of("a2"), "arb", TaskStatus.PUBLISHED, true, "mgr", null, null));

        AppResult<TaskDetailView> result = service.detail("cred-a1", "t1");

        assertFalse(result.success());
        assertEquals("无权查看该任务", result.message());
    }

    @Test
    void detailRejectsMissingTask() {
        AppResult<TaskDetailView> result = service.detail("cred-mgr", "missing");

        assertFalse(result.success());
        assertEquals("任务不存在", result.message());
    }

    @Test
    void progressUsesRepositoryProgressAndSubmissions() {
        tasks.put(task("t1", TaskStatus.STRUCTURED_PENDING));
        tasks.progress = new TaskProgress(2, 1, 1, 3, 0, 0.0);
        tasks.assignments.put("t1", new TaskAssignment("t1", List.of("a1", "a2"), "arb",
                LocalDateTime.of(2026, 1, 1, 9, 0)));
        LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 28, 14, 20, 0);
        annotations.submissions.add(new Submission("s1", "t1", "d1", RoundType.CUTTING, "r1",
                "a1", submittedAt, "v1.2", "{}", SubmissionStatus.SUBMITTED, null));

        AppResult<TaskProgressView> result = service.progress("cred-mgr", "t1");

        assertTrue(result.success());
        TaskProgressView view = result.data();
        assertEquals(2, view.due());
        assertEquals(1, view.submitted());
        assertEquals(1, view.unsubmitted());
        assertEquals(3, view.conflictCount());
        assertEquals(0, view.confirmedCount());
        assertEquals(2, view.annotators().size());
        AnnotatorProgress first = view.annotators().get(0);
        assertEquals("a1", first.id());
        assertEquals("张标注", first.name());
        assertEquals("已提交", first.status());
        assertEquals("2026-09-28 14:20:00", first.submittedAt());
        AnnotatorProgress second = view.annotators().get(1);
        assertEquals("a2", second.id());
        assertEquals("未提交", second.status());
        assertNull(second.submittedAt());
    }

    @Test
    void rejectsDisabledAccount() {
        identities.enabled = false;

        AppResult<TaskListView> result = service.myTasks("cred-mgr", SystemType.ANNOTATION, 1, 20);

        assertFalse(result.success());
        assertEquals("账号已停用，请联系管理员", result.message());
    }

    @Test
    void missingCredentialThrowsSessionExpired() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.detail("no-such-cred", "t1"));

        assertEquals("登录已失效，请重新登录", error.getMessage());
    }

    private CreateTaskCommand createCommand(boolean draft) {
        return new CreateTaskCommand("合同标注", "说明", "2030-01-01 10:00:00", "v1.2",
                "d1", List.of("a1", "a2"), "arb", draft);
    }

    private static Account account(String id, String login, String name, IdentityType identity) {
        return new Account(id, login, "pw", name, "课程组", AccountStatus.ENABLED,
                List.of(new Role(identity.name(), identity.name(), SystemType.ANNOTATION)));
    }

    private static AnnotationTask task(String id, TaskStatus status) {
        return new AnnotationTask(id, SystemType.ANNOTATION, "合同标注", "说明",
                LocalDateTime.of(2030, 1, 1, 10, 0), "v1.2", List.of("d1"),
                List.of("a1", "a2"), "arb", status, true, "mgr", null, null);
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, LoginUser> saved = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            saved.put(user.credential(), user);
            return user.credential();
        }

        @Override
        public Optional<LoginUser> findByCredential(String credential) {
            return Optional.ofNullable(saved.get(credential));
        }

        @Override
        public void delete(String credential) {
            saved.remove(credential);
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts;

        private FakeAccountRepository(List<Account> accounts) {
            this.accounts = accounts.stream().collect(Collectors.toMap(Account::accountId, a -> a));
        }

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(accounts.get(accountId));
        }

        @Override
        public Optional<Account> findByAccount(String account) {
            return accounts.values().stream().filter(a -> a.account().equals(account)).findFirst();
        }

        @Override
        public PageResult<Account> search(String keyword, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Account> search(String keyword, IdentityType identity, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String save(Account account) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(String accountId, AccountStatus status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findRoles(String accountId) {
            return accounts.get(accountId).roles();
        }

        @Override
        public void saveRoles(String accountId, List<Role> roles, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveChangeLog(AccountChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeIdentityRule implements IdentityRule {
        private boolean enabled = true;

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return account.roles().stream().anyMatch(role -> role.code().equals(identity.name()));
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return DataScope.OWN_TASKS;
        }

        @Override
        public boolean isEnabled(Account account) {
            return enabled;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            return List.of();
        }
    }

    private static final class FakeTaskRepository implements TaskRepository {
        private final Map<String, AnnotationTask> tasks = new LinkedHashMap<>();
        private final Map<String, TaskAssignment> assignments = new LinkedHashMap<>();
        private final Map<String, TaskRound> rounds = new LinkedHashMap<>();
        private final List<String> closedRounds = new ArrayList<>();
        private final List<TaskChangeLog> changeLogs = new ArrayList<>();
        private TaskProgress progress = new TaskProgress(0, 0, 0, 0, 0, 0);
        private TaskStatus lastStatusUpdate;
        private boolean managerQueried;
        private boolean annotatorQueried;
        private boolean arbiterQueried;
        private boolean findAllQueried;
        private SystemType lastFindAllSystem;
        private int sequence;

        private void put(AnnotationTask task) {
            tasks.put(task.taskId(), task);
        }

        @Override
        public String save(AnnotationTask task) {
            String id = task.taskId() != null ? task.taskId() : "t" + (tasks.size() + 1);
            tasks.put(id, new AnnotationTask(id, task.system(), task.name(), task.description(),
                    task.deadline(), task.guideVersion(), task.documentIds(), task.annotatorIds(),
                    task.arbiterId(), task.status(), task.draftAllowed(), task.publisherId(),
                    task.publishedAt(), task.withdrawReason()));
            return id;
        }

        @Override
        public Optional<AnnotationTask> findById(String taskId) {
            return Optional.ofNullable(tasks.get(taskId));
        }

        @Override
        public void update(String taskId, AnnotationTask changes, String operatorId, LocalDateTime time) {
            AnnotationTask old = tasks.get(taskId);
            tasks.put(taskId, new AnnotationTask(taskId, old.system(), changes.name(), changes.description(),
                    changes.deadline(), changes.guideVersion(), changes.documentIds(), changes.annotatorIds(),
                    changes.arbiterId(), changes.status(), changes.draftAllowed(), old.publisherId(),
                    changes.publishedAt(), changes.withdrawReason()));
        }

        @Override
        public void updateStatus(String taskId, TaskStatus status, String operatorId, LocalDateTime time) {
            AnnotationTask old = tasks.get(taskId);
            lastStatusUpdate = status;
            tasks.put(taskId, new AnnotationTask(taskId, old.system(), old.name(), old.description(),
                    old.deadline(), old.guideVersion(), old.documentIds(), old.annotatorIds(),
                    old.arbiterId(), status, old.draftAllowed(), old.publisherId(),
                    old.publishedAt(), old.withdrawReason()));
        }

        @Override
        public void saveAssignment(String taskId, List<String> annotatorIds, String arbiterId, LocalDateTime time) {
            assignments.put(taskId, new TaskAssignment(taskId, annotatorIds, arbiterId, time));
        }

        @Override
        public Optional<TaskAssignment> findAssignment(String taskId) {
            return Optional.ofNullable(assignments.get(taskId));
        }

        @Override
        public String saveRound(TaskRound round) {
            String id = round.roundId();
            if (id == null) {
                do {
                    id = "r" + (++sequence);
                } while (rounds.containsKey(id));
            }
            rounds.put(id, new TaskRound(id, round.taskId(), round.documentId(), round.type(),
                    round.sequence(), round.status(), round.openedAt(), round.closedAt()));
            return id;
        }

        @Override
        public Optional<TaskRound> findRound(String roundId) {
            return Optional.ofNullable(rounds.get(roundId));
        }

        @Override
        public Optional<TaskRound> findCurrentRound(String taskId, String documentId, RoundType type) {
            return rounds.values().stream()
                    .filter(r -> r.taskId().equals(taskId) && r.documentId().equals(documentId))
                    .filter(r -> type == null || r.type() == type)
                    .filter(r -> r.closedAt() == null)
                    .max(Comparator.comparingInt(TaskRound::sequence));
        }

        @Override
        public List<TaskRound> findRounds(String taskId, String documentId) {
            return rounds.values().stream()
                    .filter(r -> r.taskId().equals(taskId) && r.documentId().equals(documentId))
                    .sorted(Comparator.comparingInt(TaskRound::sequence))
                    .toList();
        }

        @Override
        public void closeRound(String roundId, LocalDateTime time) {
            TaskRound old = rounds.get(roundId);
            rounds.put(roundId, new TaskRound(old.roundId(), old.taskId(), old.documentId(), old.type(),
                    old.sequence(), "CLOSED", old.openedAt(), time));
            closedRounds.add(roundId);
        }

        @Override
        public PageResult<AnnotationTask> findByManager(String managerId, int page, int size) {
            managerQueried = true;
            return page(tasks.values().stream().filter(t -> managerId.equals(t.publisherId())).toList(), page, size);
        }

        @Override
        public PageResult<AnnotationTask> findByArbiter(String arbiterId, int page, int size) {
            arbiterQueried = true;
            return page(tasks.values().stream().filter(t -> arbiterId.equals(t.arbiterId())).toList(), page, size);
        }

        @Override
        public PageResult<AnnotationTask> findByAnnotator(String annotatorId, int page, int size) {
            annotatorQueried = true;
            return page(tasks.values().stream()
                    .filter(t -> t.annotatorIds().contains(annotatorId)).toList(), page, size);
        }

        @Override
        public PageResult<AnnotationTask> findAll(SystemType system, int page, int size) {
            findAllQueried = true;
            lastFindAllSystem = system;
            return page(tasks.values().stream().filter(t -> t.system() == system).toList(), page, size);
        }

        @Override
        public TaskProgress findProgress(String taskId) {
            return progress;
        }

        @Override
        public void saveChangeLog(TaskChangeLog log) {
            changeLogs.add(log);
        }

        private static PageResult<AnnotationTask> page(List<AnnotationTask> all, int page, int size) {
            int from = Math.max(0, (page - 1) * size);
            int to = Math.min(all.size(), from + size);
            List<AnnotationTask> records = from >= all.size() ? List.of() : all.subList(from, to);
            return new PageResult<>(records, all.size(), page, size);
        }
    }

    private static final class FakeAnnotationRepository implements AnnotationRepository {
        private final List<Submission> submissions = new ArrayList<>();

        @Override
        public List<Submission> findSubmissions(String taskId, String documentId, RoundType round, String annotatorId) {
            return submissions.stream()
                    .filter(s -> s.taskId().equals(taskId))
                    .filter(s -> documentId == null || documentId.equals(s.documentId()))
                    .filter(s -> round == null || round == s.round())
                    .filter(s -> annotatorId == null || annotatorId.equals(s.annotatorId()))
                    .toList();
        }

        @Override
        public String saveCutVersion(CutVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CutVersion> findCutVersion(String versionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CutVersion> findCutVersion(String taskId, String documentId, String annotatorId, RoundType round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CutVersion> findCutVersionByRound(String roundId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveElements(String cutVersionId, List<Element> elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Element> findElements(String cutVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Element> findElementsByVersion(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findLatestElementVersion(String cutVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveRelations(String elementVersionId, List<Relation> relations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Relation> findRelations(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Relation> findRelationsByVersion(String relationVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findLatestRelationVersion(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveDiagram(Diagram diagram) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Diagram> findDiagram(String diagramId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Diagram> findDiagramByRelationVersion(String relationVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveDraft(Draft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Draft> findDraft(String taskId, String documentId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Draft> findDraftByRound(String roundId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveSubmission(Submission submission, String requestId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Submission> findSubmissionsByRound(String roundId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateSubmissionStatus(String submissionId, SubmissionStatus status, String returnReason, String auditNote) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeGuideRepository implements GuideRepository {
        private final Map<String, String> references = new HashMap<>();

        @Override
        public Optional<GuideVersion> findVersion(String version) {
            return Optional.empty();
        }

        @Override
        public List<GuideVersion> findAvailable() {
            return List.of();
        }

        @Override
        public String saveVersion(GuideVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveReference(String objectType, String objectId, String version) {
            references.put(objectType + "|" + objectId, version);
        }

        @Override
        public Optional<String> findReference(String objectType, String objectId) {
            return Optional.ofNullable(references.get(objectType + "|" + objectId));
        }

        @Override
        public void saveReadingRecord(ReadingRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ReadingRecord> findReadingRecord(String userId, String version) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTaskRule implements TaskRule {
        private List<String> missing = List.of();
        private TaskStatus lastCurrent;
        private TaskAction lastAction;

        @Override
        public String recognizeDocumentType(String selectedType, byte[] file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TextRange locateReasoning(String text, String documentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CaseInfo parseCaseInfo(String text, String documentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkCaseInfo(String documentType, CaseInfo info) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkPublish(AnnotationTask draft) {
            return missing;
        }

        @Override
        public TaskStatus nextStatus(TaskStatus current, TaskAction action) {
            lastCurrent = current;
            lastAction = action;
            return switch (action) {
                case PUBLISH -> TaskStatus.PUBLISHED;
                case WITHDRAW -> TaskStatus.WITHDRAWN;
                case REOPEN -> TaskStatus.REOPENED;
                case UPDATE -> current;
                default -> current;
            };
        }

        @Override
        public TaskProgress progress(String taskId, List<Submission> submissions) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canSubmit(LocalDateTime deadline, LocalDateTime now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasEnoughAnnotators(List<String> annotatorIds) {
            return annotatorIds != null && annotatorIds.size() >= 2;
        }

        @Override
        public boolean hasSingleArbiter(String arbiterId) {
            return arbiterId != null && !arbiterId.isBlank();
        }
    }
}
