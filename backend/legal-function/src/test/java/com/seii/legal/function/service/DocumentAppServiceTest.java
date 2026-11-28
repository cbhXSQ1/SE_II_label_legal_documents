package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.Document;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.DocumentRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.document.CaseInfoView;
import com.seii.legal.function.dto.document.DocumentDetailView;
import com.seii.legal.function.dto.document.DocumentListItemView;
import com.seii.legal.function.dto.document.DocumentUploadResult;
import com.seii.legal.function.dto.document.SaveCaseInfoCommand;
import com.seii.legal.function.dto.document.UploadDocumentCommand;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.nio.charset.StandardCharsets;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentAppServiceTest {

    private static final String TEXT = "本院认为，依法成立的合同自成立时生效。判决如下：驳回上诉。";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 9, 30);

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeDocumentRepository documents = new FakeDocumentRepository();
    private final FakeFileRepository files = new FakeFileRepository();
    private final FakeTaskRule tasks = new FakeTaskRule();
    private final FakeIdentityRule identities = new FakeIdentityRule(true, Set.of(IdentityType.TASK_MANAGER));
    private final DocumentAppService service = new DocumentAppService(
            sessions, accounts, documents, files, tasks, identities);

    DocumentAppServiceTest() {
        sessions.saved.put("cred-1", new LoginUser("manager", SystemType.ANNOTATION,
                IdentityType.TASK_MANAGER, "cred-1", LocalDateTime.now().plusHours(1)));
        accounts.accounts.add(new Account("manager", "manager", "secret", "任务管理员", "课程组",
                AccountStatus.ENABLED, List.of(new Role("TASK_MANAGER", "任务管理者", SystemType.ANNOTATION))));
        tasks.range = new TaskRule.TextRange(TEXT.indexOf("本院认为"), TEXT.indexOf("判决如下"));
        tasks.caseInfo = new CaseInfo("(2026)京01民终1号", "合同效力案", "北京市第一中级人民法院",
                "张三", "甲、乙", "合同纠纷", "2026-01-01", "");
        documents.saveId = "doc-1";
        files.saveId = "file-1";
    }

    @Test
    void uploadSavesFileDocumentAndParsedCaseInfo() {
        AppResult<DocumentUploadResult> result = service.upload("cred-1",
                new UploadDocumentCommand("判决书.pdf", "pdf", TEXT, "原始判决书"));

        assertTrue(result.success());
        assertEquals("doc-1", result.data().documentId());
        assertEquals("合同效力案", result.data().title());
        assertEquals("(2026)京01民终1号", result.data().caseNo());
        assertEquals("本院认为，依法成立的合同自成立时生效。", result.data().reasonText());
        assertFalse(result.data().duplicated());
        assertEquals(1, files.saveCalls);
        assertEquals(1, documents.saveCalls);
        assertEquals(1, documents.caseInfoSaveCalls);
        assertEquals("file-1", documents.saved.fileId());
        assertEquals("原始判决书", documents.saved.documentType());
        assertEquals("manager", documents.saved.uploaderId());
        assertEquals(SystemType.ANNOTATION, documents.saved.sourceSystem());
        assertEquals("(2026)京01民终1号", documents.savedCaseInfo.caseNo());
        assertTrue(documents.savedAutoParsed);
        assertFalse(documents.savedManualEdited);
        assertEquals("manager", documents.savedCaseInfoOperator);
    }

    @Test
    void uploadReturnsExistingDocumentWhenCaseNoIsDuplicated() {
        Document existing = document("doc-old", "旧案", "(2026)京01民终1号", "原始判决书", TEXT);
        documents.documents.put(existing.documentId(), existing);
        documents.caseInfos.put("doc-old", tasks.caseInfo);

        AppResult<DocumentUploadResult> result = service.upload("cred-1",
                new UploadDocumentCommand("判决书.pdf", "pdf", TEXT, "原始判决书"));

        assertTrue(result.success());
        assertTrue(result.data().duplicated());
        assertEquals("doc-old", result.data().duplicateDocumentId());
        assertEquals("doc-old", result.data().documentId());
        assertEquals(0, files.saveCalls);
        assertEquals(0, documents.saveCalls);
        assertEquals(0, documents.caseInfoSaveCalls);
    }

    @Test
    void uploadReadsContentFromStoredFile() {
        byte[] bytes = TEXT.getBytes(StandardCharsets.UTF_8);
        files.files.put("stored-1", new StoredFile("stored-1", "案例.docx", "docx", bytes,
                "manager", NOW, "DOCUMENT", null));

        AppResult<DocumentUploadResult> result = service.upload("cred-1",
                new UploadDocumentCommand("stored-1", "原始判决书"));

        assertTrue(result.success());
        assertEquals(0, files.saveCalls);
        assertEquals("stored-1", documents.saved.fileId());
        assertEquals(TEXT, documents.saved.content());
    }

    @Test
    void uploadRejectsIdentityWithoutRole() {
        sessions.saved.put("cred-student", new LoginUser("student", SystemType.ANNOTATION,
                IdentityType.STUDENT, "cred-student", LocalDateTime.now().plusHours(1)));
        accounts.accounts.add(new Account("student", "student", "secret", "学生", "班级",
                AccountStatus.ENABLED, List.of(new Role("STUDENT", "学生", SystemType.TEACHING))));

        AppResult<DocumentUploadResult> result = service.upload("cred-student",
                new UploadDocumentCommand("判决书.pdf", "pdf", TEXT, "原始判决书"));

        assertFalse(result.success());
        assertEquals("只有任务管理者或教师可以管理文书", result.message());
        assertEquals(0, files.saveCalls);
        assertEquals(0, documents.saveCalls);
    }

    @Test
    void uploadRejectsMissingSession() {
        AppResult<DocumentUploadResult> result = service.upload("bad",
                new UploadDocumentCommand("判决书.pdf", "pdf", TEXT, "原始判决书"));

        assertFalse(result.success());
        assertEquals("登录已失效，请重新登录", result.message());
    }

    @Test
    void detailReturnsReasoningAndCaseInfo() {
        Document document = document("doc-1", "合同效力案", "(2026)京01民终1号", "原始判决书", TEXT);
        documents.documents.put("doc-1", document);
        documents.caseInfos.put("doc-1", tasks.caseInfo);

        AppResult<DocumentDetailView> result = service.detail("cred-1", "doc-1");

        assertTrue(result.success());
        assertEquals("doc-1", result.data().id());
        assertEquals("合同效力案", result.data().title());
        assertEquals("原始判决书", result.data().type());
        assertEquals("本院认为，依法成立的合同自成立时生效。", result.data().reasonText());
        assertEquals("(2026)京01民终1号", result.data().caseInfo().caseNo());
        assertEquals("manager", result.data().filledBy());
        assertEquals(NOW, result.data().filledAt());
    }

    @Test
    void detailRejectsUnknownDocument() {
        AppResult<DocumentDetailView> result = service.detail("cred-1", "missing");

        assertFalse(result.success());
        assertEquals("文书不存在", result.message());
    }

    @Test
    void saveCaseInfoChecksAndStoresManualEdits() {
        documents.documents.put("doc-1", document("doc-1", "合同效力案", "old", "原始判决书", TEXT));
        CaseInfoView view = new CaseInfoView("(2026)京01民终1号", "合同效力案", "北京市第一中级人民法院",
                "张三", "甲、乙", "合同纠纷", "2026-01-01", "备注");

        AppResult<Void> result = service.saveCaseInfo("cred-1", "doc-1", new SaveCaseInfoCommand(view));

        assertTrue(result.success());
        assertEquals(1, documents.caseInfoSaveCalls);
        assertEquals("(2026)京01民终1号", documents.savedCaseInfo.caseNo());
        assertEquals("备注", documents.savedCaseInfo.extra());
        assertFalse(documents.savedAutoParsed);
        assertTrue(documents.savedManualEdited);
        assertEquals("manager", documents.savedCaseInfoOperator);
    }

    @Test
    void saveCaseInfoReturnsMissingFields() {
        documents.documents.put("doc-1", document("doc-1", "合同效力案", "old", "原始判决书", TEXT));
        tasks.missingFields = List.of("案号", "裁判法院");
        CaseInfoView view = new CaseInfoView(null, null, null, null, null, null, null, null);

        AppResult<Void> result = service.saveCaseInfo("cred-1", "doc-1", new SaveCaseInfoCommand(view));

        assertFalse(result.success());
        assertEquals("案号、裁判法院", result.message());
        assertEquals(0, documents.caseInfoSaveCalls);
    }

    @Test
    void caseInfoReadsStoredInfo() {
        documents.documents.put("doc-1", document("doc-1", "合同效力案", "old", "原始判决书", TEXT));
        documents.caseInfos.put("doc-1", tasks.caseInfo);

        AppResult<CaseInfoView> result = service.caseInfo("cred-1", "doc-1");

        assertTrue(result.success());
        assertEquals("(2026)京01民终1号", result.data().caseNo());
        assertEquals("北京市第一中级人民法院", result.data().court());
    }

    @Test
    void listMapsDocumentPageAndPassesSystem() {
        List<Document> records = List.of(
                document("doc-1", "合同效力案", "no-1", "原始判决书", TEXT),
                document("doc-2", "指导案例", "no-2", "指导性案例", TEXT));
        documents.searchResult = new PageResult<>(records, 7, 2, 5);

        AppResult<PageResult<DocumentListItemView>> result = service.list("cred-1", "合同", SystemType.ANNOTATION, 2, 5);

        assertTrue(result.success());
        assertEquals(7, result.data().total());
        assertEquals(2, result.data().page());
        assertEquals(5, result.data().size());
        assertEquals(2, result.data().records().size());
        assertEquals("doc-1", result.data().records().get(0).id());
        assertEquals("原始判决书", result.data().records().get(0).type());
        assertNull(result.data().records().get(0).taskId());
        assertEquals("合同", documents.lastKeyword);
        assertEquals(SystemType.ANNOTATION, documents.lastSystem);
    }

    private static Document document(String id, String title, String caseNo, String type, String content) {
        return new Document(id, title, caseNo, type, content, "file-1", "manager", NOW,
                "全部", SystemType.ANNOTATION);
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
        private final List<Account> accounts = new ArrayList<>();

        @Override
        public Optional<Account> findById(String accountId) {
            return accounts.stream().filter(a -> a.accountId().equals(accountId)).findFirst();
        }

        @Override
        public Optional<Account> findByAccount(String account) {
            return accounts.stream().filter(a -> a.account().equals(account)).findFirst();
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
            throw new UnsupportedOperationException();
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

    private static final class FakeDocumentRepository implements DocumentRepository {
        private final Map<String, Document> documents = new HashMap<>();
        private final Map<String, CaseInfo> caseInfos = new HashMap<>();
        private String saveId;
        private Document saved;
        private CaseInfo savedCaseInfo;
        private boolean savedAutoParsed;
        private boolean savedManualEdited;
        private String savedCaseInfoOperator;
        private int saveCalls;
        private int caseInfoSaveCalls;
        private PageResult<Document> searchResult = new PageResult<>(List.of(), 0, 1, 20);
        private String lastKeyword;
        private SystemType lastSystem;

        @Override
        public String save(Document document) {
            saveCalls++;
            saved = document;
            String id = document.documentId() == null ? saveId : document.documentId();
            documents.put(id, new Document(id, document.title(), document.caseNo(), document.documentType(),
                    document.content(), document.fileId(), document.uploaderId(), document.uploadedAt(),
                    document.scope(), document.sourceSystem()));
            return id;
        }

        @Override
        public Optional<Document> findById(String documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public Optional<Document> findByCaseNo(String caseNo) {
            return documents.values().stream().filter(d -> caseNo.equals(d.caseNo())).findFirst();
        }

        @Override
        public PageResult<Document> search(String keyword, SystemType system, int page, int size) {
            lastKeyword = keyword;
            lastSystem = system;
            return searchResult;
        }

        @Override
        public void saveCaseInfo(String documentId, CaseInfo caseInfo, boolean autoParsed,
                                 boolean manualEdited, String operatorId, LocalDateTime time) {
            caseInfoSaveCalls++;
            savedCaseInfo = caseInfo;
            savedAutoParsed = autoParsed;
            savedManualEdited = manualEdited;
            savedCaseInfoOperator = operatorId;
            caseInfos.put(documentId, caseInfo);
        }

        @Override
        public Optional<CaseInfo> findCaseInfo(String documentId) {
            return Optional.ofNullable(caseInfos.get(documentId));
        }

        @Override
        public void saveRangeChange(String documentId, String beforeRange, String afterRange,
                                    String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeFileRepository implements FileRepository {
        private final Map<String, StoredFile> files = new HashMap<>();
        private String saveId;
        private int saveCalls;

        @Override
        public String save(String fileName, String format, byte[] content, String uploaderId,
                           LocalDateTime time, String type) {
            saveCalls++;
            files.put(saveId, new StoredFile(saveId, fileName, format, content, uploaderId, time, type, null));
            return saveId;
        }

        @Override
        public Optional<StoredFile> find(String fileId) {
            return Optional.ofNullable(files.get(fileId));
        }

        @Override
        public void delete(String fileId) {
            files.remove(fileId);
        }
    }

    private static final class FakeTaskRule implements TaskRule {
        private TaskRule.TextRange range = new TaskRule.TextRange(0, 0);
        private CaseInfo caseInfo;
        private List<String> missingFields = List.of();

        @Override
        public String recognizeDocumentType(String selectedType, byte[] file) {
            return selectedType;
        }

        @Override
        public TextRange locateReasoning(String text, String documentType) {
            return range;
        }

        @Override
        public CaseInfo parseCaseInfo(String text, String documentType) {
            return caseInfo;
        }

        @Override
        public List<String> checkCaseInfo(String documentType, CaseInfo info) {
            return missingFields;
        }

        @Override
        public List<String> checkPublish(AnnotationTask draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskStatus nextStatus(TaskStatus current, TaskAction action) {
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasSingleArbiter(String arbiterId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeIdentityRule implements IdentityRule {
        private boolean enabled;
        private final Set<IdentityType> roles;

        private FakeIdentityRule(boolean enabled, Set<IdentityType> roles) {
            this.enabled = enabled;
            this.roles = roles;
        }

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return roles.contains(identity);
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return DataScope.ALL_TASKS;
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
}
