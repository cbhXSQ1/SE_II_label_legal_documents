package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.data.entity.ExpertReply;
import com.seii.legal.data.entity.ExportFile;
import com.seii.legal.data.entity.ExportRecord;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.ImportPreviewRecord;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.entity.TaskAssignment;
import com.seii.legal.data.entity.TaskChangeLog;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.ExportRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.export.ExportCommand;
import com.seii.legal.function.dto.export.ExportResult;
import com.seii.legal.function.dto.export.ExportTaskListView;
import com.seii.legal.function.dto.export.ImportCommand;
import com.seii.legal.function.dto.export.ImportResult;
import com.seii.legal.rule.export.ExportRule;
import com.seii.legal.rule.identity.IdentityRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportAppServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 10, 0);

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeIdentityRule identities = new FakeIdentityRule();
    private final FakeTaskRepository tasks = new FakeTaskRepository();
    private final FakeExportRepository exports = new FakeExportRepository();
    private final FakeFileRepository files = new FakeFileRepository();
    private final FakeAdjudicationRepository adjudications = new FakeAdjudicationRepository();
    private final FakeExportRule rule = new FakeExportRule();
    private final ExportAppService service = new ExportAppService(
            sessions, accounts, identities, tasks, exports, files, adjudications, rule);

    @Test
    void exportableTasksFiltersConfirmedTasksOfManager() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-mgr", TaskStatus.CONFIRMED, "d1");
        task("t2", "u-mgr", TaskStatus.PUBLISHED, "d1");
        task("t3", "u-other", TaskStatus.CONFIRMED, "d1");
        adjudications.finalVersions.put("t1|d1", finalVersion("t1", "d1"));

        AppResult<ExportTaskListView> result = service.exportableTasks("cred");

        assertTrue(result.success());
        assertEquals(1, result.data().tasks().size());
        assertEquals("t1", result.data().tasks().get(0).id());
        assertEquals(2, result.data().tasks().get(0).annotatorCount());
        assertEquals("2026-11-28 10:00:00", result.data().tasks().get(0).confirmedAt());
        assertEquals("u-mgr", tasks.lastManagerId);
    }

    @Test
    void exportableTasksRejectsAnnotator() {
        login("u-ann", IdentityType.ANNOTATOR);

        AppResult<ExportTaskListView> result = service.exportableTasks("cred");

        assertFalse(result.success());
        assertEquals("只有任务管理者可以导出", result.message());
    }

    @Test
    void createSavesRecordAndFile() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-mgr", TaskStatus.CONFIRMED, "d1");
        adjudications.finalVersions.put("t1|d1", finalVersion("t1", "d1"));

        AppResult<ExportResult> result = service.create("cred",
                new ExportCommand("t1", "标注数据", "JSON", "PNG", "out.zip", "req-1"));

        assertTrue(result.success());
        assertEquals("exp-1", result.data().exportId());
        assertTrue(result.data().downloadUrl().contains("file-1"));
        assertNotNull(exports.lastRecord);
        assertEquals("out.zip", exports.lastRecord.fileName());
        assertEquals("req-1", exports.lastRequestId);
        assertNotNull(exports.lastFile);
        assertEquals("exp-1", exports.lastFile.exportId());
        assertEquals(1, rule.assembleCalls);
        assertEquals(1, rule.toJsonCalls);
    }

    @Test
    void createExcelUsesExcelMapper() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-mgr", TaskStatus.CONFIRMED, "d1");
        adjudications.finalVersions.put("t1|d1", finalVersion("t1", "d1"));

        AppResult<ExportResult> result = service.create("cred",
                new ExportCommand("t1", "两者", "Excel", "PNG", "out.xlsx", "req-2"));

        assertTrue(result.success());
        assertEquals(1, rule.toExcelCalls);
        assertEquals(0, rule.toJsonCalls);
    }

    @Test
    void createRejectsDuplicateFileName() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-mgr", TaskStatus.CONFIRMED, "d1");
        adjudications.finalVersions.put("t1|d1", finalVersion("t1", "d1"));
        exports.records = List.of(new ExportRecord("e1", "u-mgr", NOW, "t1", "v1", "out.zip", "JSON"));

        AppResult<ExportResult> result = service.create("cred",
                new ExportCommand("t1", "标注数据", "JSON", "PNG", "out.zip", "req-3"));

        assertFalse(result.success());
        assertEquals("文件名已经存在", result.message());
        assertNull(exports.lastRecord);
    }

    @Test
    void createRejectsUnconfirmedTask() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-mgr", TaskStatus.STRUCTURED_PENDING, "d1");

        AppResult<ExportResult> result = service.create("cred",
                new ExportCommand("t1", "标注数据", "JSON", "PNG", "out.zip", "req-4"));

        assertFalse(result.success());
        assertEquals("请先确认最终版本", result.message());
    }

    @Test
    void createRejectsMissingFinalVersion() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-mgr", TaskStatus.CONFIRMED, "d1");

        AppResult<ExportResult> result = service.create("cred",
                new ExportCommand("t1", "标注数据", "JSON", "PNG", "out.zip", "req-5"));

        assertFalse(result.success());
        assertEquals("最终版本不存在", result.message());
    }

    @Test
    void createRejectsOtherManagersTask() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        task("t1", "u-other", TaskStatus.CONFIRMED, "d1");

        AppResult<ExportResult> result = service.create("cred",
                new ExportCommand("t1", "标注数据", "JSON", "PNG", "out.zip", "req-6"));

        assertFalse(result.success());
        assertEquals("只能导出自己创建的任务", result.message());
    }

    @Test
    void importSavesNewDraftWhenAllRowsValid() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        files.files.put("f1", storedFile("f1"));
        rule.preview = new ExportRule.ImportPreview("导入任务", 2, 2, true, List.of());

        AppResult<ImportResult> result = service.importFile("cred", new ImportCommand("f1"));

        assertTrue(result.success());
        assertNotNull(result.data().taskId());
        assertEquals("导入任务", result.data().preview().taskName());
        AnnotationTask draft = tasks.lastSaved;
        assertNotNull(draft);
        assertEquals("导入任务", draft.name());
        assertEquals(SystemType.ANNOTATION, draft.system());
        assertEquals(TaskStatus.DRAFT, draft.status());
        assertEquals("u-mgr", draft.publisherId());
        assertEquals(1, tasks.saveCalls);
    }

    @Test
    void importDoesNotSaveWhenAnyRowInvalid() {
        login("u-mgr", IdentityType.TASK_MANAGER);
        files.files.put("f1", storedFile("f1"));
        rule.preview = new ExportRule.ImportPreview("导入任务", 2, 1, false, List.of("第2行缺少文书"));

        AppResult<ImportResult> result = service.importFile("cred", new ImportCommand("f1"));

        assertFalse(result.success());
        assertTrue(result.message().contains("第2行缺少文书"));
        assertEquals(0, tasks.saveCalls);
        assertNull(tasks.lastSaved);
    }

    @Test
    void importRejectsMissingFile() {
        login("u-mgr", IdentityType.TASK_MANAGER);

        AppResult<ImportResult> result = service.importFile("cred", new ImportCommand("missing"));

        assertFalse(result.success());
        assertEquals("文件不存在", result.message());
    }

    @Test
    void importRejectsNonManager() {
        login("u-ann", IdentityType.ANNOTATOR);

        AppResult<ImportResult> result = service.importFile("cred", new ImportCommand("f1"));

        assertFalse(result.success());
        assertEquals("只有任务管理者可以导出", result.message());
    }

    private void login(String accountId, IdentityType identity) {
        sessions.users.put("cred", new LoginUser(accountId, SystemType.ANNOTATION, identity,
                "cred", NOW.plusHours(1)));
        accounts.accounts.put(accountId, new Account(accountId, accountId, "pw", accountId, "课程组",
                AccountStatus.ENABLED, List.of(new Role(identity.name(), identity.name(), SystemType.ANNOTATION))));
        identities.roles = Set.of(identity);
    }

    private void task(String taskId, String publisherId, TaskStatus status, String documentId) {
        tasks.tasks.put(taskId, new AnnotationTask(taskId, SystemType.ANNOTATION, "任务" + taskId, "说明",
                NOW.plusDays(1), "v1", List.of(documentId), List.of("a1", "a2"), "u-arb",
                status, true, publisherId, NOW.minusDays(1), null));
    }

    private static FinalVersion finalVersion(String taskId, String documentId) {
        return new FinalVersion("fv-" + taskId, taskId, documentId, "r1", VersionType.FINAL,
                "c1", "e1", "rel1", "dg1", "v1", "u-arb", NOW, "CONFIRMED");
    }

    private static StoredFile storedFile(String fileId) {
        return new StoredFile(fileId, "import.zip", "ZIP", new byte[]{1, 2, 3}, "u-mgr", NOW, "EXPORT", null);
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, LoginUser> users = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            users.put(user.credential(), user);
            return user.credential();
        }

        @Override
        public Optional<LoginUser> findByCredential(String credential) {
            return Optional.ofNullable(users.get(credential));
        }

        @Override
        public void delete(String credential) {
            users.remove(credential);
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new HashMap<>();

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(accounts.get(accountId));
        }

        @Override
        public Optional<Account> findByAccount(String account) {
            throw new UnsupportedOperationException();
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
        public void saveChangeLog(com.seii.legal.data.entity.AccountChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeIdentityRule implements IdentityRule {
        private Set<IdentityType> roles = Set.of();

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return roles.contains(identity);
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return DataScope.OWN_TASKS;
        }

        @Override
        public boolean isEnabled(Account account) {
            return account.status() == AccountStatus.ENABLED;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTaskRepository implements TaskRepository {
        private final Map<String, AnnotationTask> tasks = new HashMap<>();
        private AnnotationTask lastSaved;
        private String lastManagerId;
        private int saveCalls;

        @Override
        public String save(AnnotationTask task) {
            saveCalls++;
            lastSaved = task;
            String id = "t-import";
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
        public PageResult<AnnotationTask> findByManager(String managerId, int page, int size) {
            lastManagerId = managerId;
            return new PageResult<>(new ArrayList<>(tasks.values()), tasks.size(), page, size);
        }

        @Override
        public void update(String taskId, AnnotationTask changes, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(String taskId, TaskStatus status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAssignment(String taskId, List<String> annotatorIds, String arbiterId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TaskAssignment> findAssignment(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveRound(TaskRound round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TaskRound> findRound(String roundId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TaskRound> findCurrentRound(String taskId, String documentId, RoundType type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TaskRound> findRounds(String taskId, String documentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closeRound(String roundId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findByArbiter(String arbiterId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findByAnnotator(String annotatorId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findAll(SystemType system, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskProgress findProgress(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveChangeLog(TaskChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeExportRepository implements ExportRepository {
        private List<ExportRecord> records = List.of();
        private ExportRecord lastRecord;
        private ExportFile lastFile;
        private String lastRequestId;

        @Override
        public String saveRecord(ExportRecord record, String requestId) {
            lastRecord = record;
            lastRequestId = requestId;
            return "exp-1";
        }

        @Override
        public String saveFile(ExportFile file, String exportId) {
            lastFile = file;
            return "file-1";
        }

        @Override
        public List<ExportRecord> findRecords(String taskId) {
            return records;
        }

        @Override
        public String saveImportPreview(ImportPreviewRecord preview) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ImportPreviewRecord> findImportPreview(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteImportPreview(String taskId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeFileRepository implements FileRepository {
        private final Map<String, StoredFile> files = new HashMap<>();

        @Override
        public String save(String fileName, String format, byte[] content, String uploaderId,
                           LocalDateTime time, String type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StoredFile> find(String fileId) {
            return Optional.ofNullable(files.get(fileId));
        }

        @Override
        public void delete(String fileId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeAdjudicationRepository implements AdjudicationRepository {
        private final Map<String, FinalVersion> finalVersions = new HashMap<>();

        @Override
        public Optional<FinalVersion> findFinalVersion(String taskId, String documentId) {
            return Optional.ofNullable(finalVersions.get(taskId + "|" + documentId));
        }

        @Override
        public Optional<FinalVersion> findFinalVersion(String taskId, String documentId, VersionType versionType) {
            return findFinalVersion(taskId, documentId);
        }

        @Override
        public Optional<FinalVersion> findFinalVersionByRound(String roundId, VersionType versionType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveDecision(AdjudicationRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AdjudicationRecord> findDecisions(String taskId, String documentId, RoundType round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AdjudicationRecord> findDecisionsByRound(String roundId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveExpertQuestion(ExpertQuestion question) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ExpertQuestion> findExpertQuestion(String questionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<ExpertQuestion> findExpertQuestions(String expertId, com.seii.legal.common.enums.QuestionStatus status,
                                                              int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveExpertReply(ExpertReply reply) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateQuestionStatus(String questionId, com.seii.legal.common.enums.QuestionStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DiagramReview> findDiagramReviews(String taskId, String documentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveDiagramReview(DiagramReview review) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveFinalVersion(FinalVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void confirmTask(String taskId, String confirmerId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeExportRule implements ExportRule {
        private ExportRule.ImportPreview preview = new ExportRule.ImportPreview(
                "导入任务", 0, 0, true, List.of());
        private int assembleCalls;
        private int toJsonCalls;
        private int toExcelCalls;

        @Override
        public String assemble(FinalVersion version, String selection) {
            assembleCalls++;
            return "assembled";
        }

        @Override
        public String toJson(FinalVersion version) {
            toJsonCalls++;
            return "{\"data\":true}";
        }

        @Override
        public String toExcel(FinalVersion version) {
            toExcelCalls++;
            return "excel";
        }

        @Override
        public boolean fileNameExists(String fileName, List<String> existing) {
            return existing.contains(fileName);
        }

        @Override
        public ImportPreview parseImport(byte[] file, String currentGuideVersion) {
            return preview;
        }
    }
}
