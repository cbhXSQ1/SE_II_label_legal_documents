package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.ApprovalRecord;
import com.seii.legal.data.entity.ClassMember;
import com.seii.legal.data.entity.ImportRecord;
import com.seii.legal.data.entity.ImportRow;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.entity.StudentAccountResult;
import com.seii.legal.data.entity.TeachingClass;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.function.dto.teachingadmin.AccountQuery;
import com.seii.legal.function.dto.teachingadmin.AccountView;
import com.seii.legal.function.dto.teachingadmin.ApplicationQuery;
import com.seii.legal.function.dto.teachingadmin.ClassQuery;
import com.seii.legal.function.dto.teachingadmin.ClassView;
import com.seii.legal.function.dto.teachingadmin.ImportCommand;
import com.seii.legal.function.dto.teachingadmin.ImportResult;
import com.seii.legal.function.dto.teachingadmin.ImportResultView;
import com.seii.legal.function.dto.teachingadmin.ReviewCommand;
import com.seii.legal.rule.identity.AccountRule;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.notification.NotificationService;
import com.seii.legal.rule.teaching.TeachingAdminRule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeachingAdminAppServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 10, 0);

    private FakeSessionRepository sessions;
    private FakeAccountRepository accounts;
    private FakeApplicationRepository applications;
    private FakeTeachingAdminRepository teaching;
    private FakeFileRepository files;
    private FakeIdentityRule identities;
    private FakeAccountRule accountRule;
    private FakeTeachingAdminRule teachingRule;
    private FakeNotificationService notifications;
    private TeachingAdminAppService service;

    @BeforeEach
    void setUp() {
        sessions = new FakeSessionRepository();
        accounts = new FakeAccountRepository();
        applications = new FakeApplicationRepository();
        teaching = new FakeTeachingAdminRepository();
        files = new FakeFileRepository();
        identities = new FakeIdentityRule();
        accountRule = new FakeAccountRule();
        teachingRule = new FakeTeachingAdminRule();
        notifications = new FakeNotificationService();
        service = new TeachingAdminAppService(sessions, accounts, applications, teaching,
                files, identities, accountRule, teachingRule, notifications);
        sessions.saved.put("cred", new LoginUser("ta1", SystemType.TEACHING, IdentityType.TEACHING_ADMIN,
                "cred", NOW.plusHours(1)));
        accounts.accounts.put("ta1", account("ta1", "admin", "教学管理员", AccountStatus.ENABLED,
                new Role("TEACHING_ADMIN", "教学管理员", SystemType.TEACHING)));
        identities.scope = DataScope.TEACHING_ACCOUNTS;
    }

    @Test
    void importRejectsNonTeachingAdmin() {
        sessions.saved.put("cred", new LoginUser("ta1", SystemType.TEACHING, IdentityType.TEACHER,
                "cred", NOW.plusHours(1)));
        files.files.put("f1", storedFile("classes.csv", "法学2501,S001,张三,T001"));

        AppResult<ImportResult> result = service.importClasses("cred", new ImportCommand("f1"));

        assertFalse(result.success());
        assertEquals("只有教学管理员可以导入班级和学生名单", result.message());
        assertTrue(teaching.savedClasses.isEmpty());
    }

    @Test
    void importRejectsOutsideTeachingAccountsScope() {
        identities.scope = DataScope.ALL_TASKS;
        files.files.put("f1", storedFile("classes.csv", "法学2501,S001,张三,T001"));

        AppResult<ImportResult> result = service.importClasses("cred", new ImportCommand("f1"));

        assertFalse(result.success());
        assertEquals("当前身份不能查看教学账号", result.message());
        assertTrue(teaching.savedClasses.isEmpty());
    }

    @Test
    void importCreatesClassesAccountsAndMembers() {
        files.files.put("f1", storedFile("classes.csv",
                "班级编号,学号,姓名,工号\n"
                        + "法学2501,S001,张三,T001\n"
                        + "法学2501,S002,李四,T001\n"
                        + "法学2502,S003,王五,T002"));
        accounts.accounts.put("teacher1", account("teacher1", "t001", "赵老师", AccountStatus.ENABLED,
                new Role("TEACHER", "教师", SystemType.TEACHING)));
        accounts.teachersByNo.put("T001", accounts.accounts.get("teacher1"));
        accounts.teachersByNo.put("T002", account("teacher2", "t002", "钱老师", AccountStatus.ENABLED,
                new Role("TEACHER", "教师", SystemType.TEACHING)));
        teachingRule.teacherIds.put("T001", "teacher1");
        teachingRule.teacherIds.put("T002", "teacher2");

        AppResult<ImportResult> result = service.importClasses("cred", new ImportCommand("f1"));

        assertTrue(result.success());
        ImportResult view = result.data();
        assertEquals(2, view.classCount());
        assertEquals(3, view.studentCount());
        assertTrue(view.missingRows().isEmpty());
        assertTrue(view.duplicateStudentNos().isEmpty());
        assertTrue(view.unmatchedTeachers().isEmpty());
        assertEquals(2, teaching.savedClasses.size());
        assertEquals(3, teaching.createdStudents.size());
        assertEquals(3, teaching.members.size());
        assertEquals(List.of("S001", "S002", "S003"), teaching.createdStudents);
        ImportRecord record = teaching.importRecords.get(view.importId());
        assertNotNull(record);
        assertEquals(2, record.classCount());
        assertEquals(3, record.studentCount());
        assertEquals("ta1", record.operatorId());
    }

    @Test
    void importReportsMissingAndDuplicateRowsButImportsValidOnes() {
        files.files.put("f1", storedFile("classes.csv",
                "法学2501,S001,张三,T001\n"
                        + "法学2501,,李四,T001\n"
                        + "法学2501,S001,王五,T001"));
        accounts.teachersByNo.put("T001", account("teacher1", "t001", "赵老师", AccountStatus.ENABLED,
                new Role("TEACHER", "教师", SystemType.TEACHING)));
        teachingRule.teacherIds.put("T001", "teacher1");
        teachingRule.rejectBlankStudentNo = true;

        AppResult<ImportResult> result = service.importClasses("cred", new ImportCommand("f1"));

        assertTrue(result.success());
        ImportResult view = result.data();
        assertEquals(1, view.studentCount());
        assertEquals(1, view.classCount());
        assertEquals(1, view.missingRows().size());
        assertEquals(2, view.missingRows().get(0).row());
        assertEquals(List.of("S001"), view.duplicateStudentNos());
        assertEquals(List.of("S001"), teaching.createdStudents);
        assertEquals(1, teaching.members.size());
    }

    @Test
    void importReportsUnmatchedTeachers() {
        files.files.put("f1", storedFile("classes.csv", "法学2501,S001,张三,T999"));
        accounts.teachersByNo.put("T999", account("teacher9", "t999", "孙老师", AccountStatus.ENABLED,
                new Role("TEACHER", "教师", SystemType.TEACHING)));

        AppResult<ImportResult> result = service.importClasses("cred", new ImportCommand("f1"));

        assertTrue(result.success());
        assertEquals(List.of("T999"), result.data().unmatchedTeachers());
        assertEquals(1, result.data().studentCount());
        assertNull(teaching.savedClasses.get(0).teacherId());
    }

    @Test
    void importFailsWhenFileMissing() {
        AppResult<ImportResult> result = service.importClasses("cred", new ImportCommand("missing"));

        assertFalse(result.success());
        assertEquals("导入文件不存在", result.message());
        assertTrue(teaching.savedClasses.isEmpty());
    }

    @Test
    void importResultReturnsSummary() {
        teaching.importRecords.put("imp-1", new ImportRecord("imp-1", "ta1", NOW, 2, 3,
                List.of(new ImportRow(5, "法学2501", "S009", "周八", "T001", List.of("缺少姓名")))));

        AppResult<ImportResultView> result = service.importResult("cred", "imp-1");

        assertTrue(result.success());
        ImportResultView view = result.data();
        assertEquals(2, view.successClasses());
        assertEquals(3, view.successStudents());
        assertEquals(1, view.failedRows().size());
        assertEquals(5, view.failedRows().get(0).row());
        assertEquals("缺少姓名", view.failedRows().get(0).reason());
        assertNull(view.downloadUrl());
    }

    @Test
    void importResultFailsWhenRecordMissing() {
        AppResult<ImportResultView> result = service.importResult("cred", "missing");

        assertFalse(result.success());
        assertEquals("导入记录不存在", result.message());
    }

    @Test
    void listClassesMapsTeacherNameAndStudentCount() {
        TeachingClass teachingClass = new TeachingClass("c1", "法学2501", "法学2501班", "teacher1", "2026秋", NOW);
        teaching.classes.put("c1", teachingClass);
        accounts.accounts.put("teacher1", account("teacher1", "t001", "赵老师", AccountStatus.ENABLED,
                new Role("TEACHER", "教师", SystemType.TEACHING)));
        teaching.classMemberMap.put("c1", List.of(
                new ClassMember("c1", "s1", "S001", NOW, "ACTIVE"),
                new ClassMember("c1", "s2", "S002", NOW, "ACTIVE")));
        teaching.classPage = new PageResult<>(List.of(teachingClass), 1, 1, 20);

        AppResult<PageResult<ClassView>> result = service.listClasses("cred",
                new ClassQuery("2026秋", "teacher1", 1, 20));

        assertTrue(result.success());
        assertEquals(1, result.data().total());
        ClassView view = result.data().records().get(0);
        assertEquals("c1", view.id());
        assertEquals("法学2501", view.code());
        assertEquals("赵老师", view.teacher());
        assertEquals(2, view.studentCount());
        assertEquals("2026秋", teaching.lastClassTerm);
        assertEquals("teacher1", teaching.lastClassTeacher);
    }

    @Test
    void listAccountsMapsIdentityAndClassAndFiltersByClass() {
        Account student = account("s1", "s001", "张三", AccountStatus.ENABLED,
                new Role("STUDENT", "学生", SystemType.TEACHING));
        Account teacher = account("teacher1", "t001", "赵老师", AccountStatus.ENABLED,
                new Role("TEACHER", "教师", SystemType.TEACHING));
        accounts.allAccounts = List.of(student, teacher);
        teaching.classes.put("c1", new TeachingClass("c1", "法学2501", "法学2501班", "teacher1", "2026秋", NOW));
        teaching.studentClasses.put("s1", "c1");
        teaching.members.add(new ClassMember("c1", "s1", "S001", NOW, "ACTIVE"));
        teaching.classMemberMap.put("c1", List.of(new ClassMember("c1", "s1", "S001", NOW, "ACTIVE")));

        AppResult<PageResult<AccountView>> result = service.listAccounts("cred",
                new AccountQuery("c1", "ENABLED", "张", 1, 20));

        assertTrue(result.success());
        assertEquals(1, result.data().records().size());
        AccountView view = result.data().records().get(0);
        assertEquals("s1", view.id());
        assertEquals("s001", view.account());
        assertEquals(IdentityType.STUDENT, view.identity());
        assertEquals(AccountStatus.ENABLED, view.status());
        assertEquals("法学2501班", view.className());
    }

    @Test
    void listApplicationsQueriesTeachingPendingWithDefaults() {
        AccountApplication application = application("a1", SystemType.TEACHING, IdentityType.TEACHER,
                ApplicationStatus.PENDING);
        applications.pendingPage = new PageResult<>(List.of(application), 1, 1, 20);

        AppResult<PageResult<AccountApplication>> result = service.listApplications("cred",
                new ApplicationQuery("PENDING", 0, 0));

        assertTrue(result.success());
        assertEquals(1, result.data().records().size());
        assertEquals(IdentityType.TEACHING_ADMIN, applications.lastApproverIdentity);
        assertEquals(SystemType.TEACHING, applications.lastSystem);
        assertEquals(1, applications.lastPage);
        assertEquals(20, applications.lastSize);
    }

    @Test
    void reviewApproveCreatesAccountRolesAndNotifies() {
        AccountApplication application = application("a1", SystemType.TEACHING, IdentityType.TEACHER,
                ApplicationStatus.PENDING);
        applications.applications.put("a1", application);
        accounts.generatedId = "new-1";

        AppResult<Void> result = service.reviewApplication("cred",
                new ReviewCommand("a1", "APPROVE", IdentityType.TEACHER, "通过"));

        assertTrue(result.success());
        assertEquals("new-1", accounts.savedAccount.accountId());
        assertEquals("new-1", accounts.savedRolesAccountId);
        assertEquals(1, accounts.savedRoles.size());
        assertEquals("TEACHER", accounts.savedRoles.get(0).code());
        assertEquals(SystemType.TEACHING, accounts.savedRoles.get(0).system());
        assertEquals(ApplicationStatus.APPROVED, applications.lastStatus);
        assertEquals("ta1", applications.lastApproverId);
        assertNotNull(applications.savedApproval);
        assertEquals("APPROVE", applications.savedApproval.action());
        assertEquals(List.of("new-1"), notifications.inAppReceivers);
    }

    @Test
    void reviewRejectsNonTeachingApplication() {
        applications.applications.put("a1",
                application("a1", SystemType.ADMIN, IdentityType.TEACHER, ApplicationStatus.PENDING));

        AppResult<Void> result = service.reviewApplication("cred",
                new ReviewCommand("a1", "APPROVE", IdentityType.TEACHER, "通过"));

        assertFalse(result.success());
        assertEquals("只能审批教学系统账号申请", result.message());
        assertNull(accounts.savedAccount);
        assertNull(applications.lastStatus);
    }

    @Test
    void reviewRejectsIdentityOutsideTeacherAndStudent() {
        applications.applications.put("a1",
                application("a1", SystemType.TEACHING, IdentityType.TEACHER, ApplicationStatus.PENDING));

        AppResult<Void> result = service.reviewApplication("cred",
                new ReviewCommand("a1", "APPROVE", IdentityType.TEACHING_ADMIN, "通过"));

        assertFalse(result.success());
        assertEquals("教学账号身份只能是教师或学生", result.message());
        assertNull(accounts.savedAccount);
    }

    @Test
    void reviewReturnUpdatesStatusWithoutCreatingAccount() {
        applications.applications.put("a1",
                application("a1", SystemType.TEACHING, IdentityType.STUDENT, ApplicationStatus.PENDING));

        AppResult<Void> result = service.reviewApplication("cred",
                new ReviewCommand("a1", "RETURN", null, "补充学号"));

        assertTrue(result.success());
        assertEquals(ApplicationStatus.RETURNED, applications.lastStatus);
        assertNull(accounts.savedAccount);
        assertNotNull(applications.savedApproval);
        assertEquals("RETURN", applications.savedApproval.action());
    }

    @Test
    void reviewFailsWhenApplicationMissing() {
        AppResult<Void> result = service.reviewApplication("cred",
                new ReviewCommand("missing", "APPROVE", IdentityType.TEACHER, "通过"));

        assertFalse(result.success());
        assertEquals("申请不存在", result.message());
    }

    private static Account account(String id, String login, String name, AccountStatus status, Role... roles) {
        return new Account(id, login, "pw", name, "课程组", status, List.of(roles));
    }

    private static AccountApplication application(String id, SystemType system, IdentityType identity,
                                                  ApplicationStatus status) {
        return new AccountApplication(id, "zhang", "pw", "张老师", "课程组", system, identity, status,
                NOW.minusDays(1), null, null, "申请");
    }

    private static StoredFile storedFile(String fileName, String content) {
        return new StoredFile("f1", fileName, "csv", content.getBytes(StandardCharsets.UTF_8),
                "ta1", NOW, "CLASS_IMPORT", null);
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
        private final Map<String, Account> accounts = new HashMap<>();
        private final Map<String, Account> teachersByNo = new HashMap<>();
        private List<Account> allAccounts = List.of();
        private Account savedAccount;
        private String savedRolesAccountId;
        private List<Role> savedRoles = List.of();
        private String generatedId = "new-1";

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(accounts.get(accountId));
        }

        @Override
        public Optional<Account> findByAccount(String account) {
            return accounts.values().stream().filter(item -> item.account().equals(account)).findFirst();
        }

        @Override
        public PageResult<Account> search(String keyword, int page, int size) {
            return new PageResult<>(allAccounts, allAccounts.size(), page, size);
        }

        @Override
        public PageResult<Account> search(String keyword, IdentityType identity, int page, int size) {
            return new PageResult<>(allAccounts, allAccounts.size(), page, size);
        }

        @Override
        public String save(Account account) {
            savedAccount = new Account(generatedId, account.account(), account.password(), account.name(),
                    account.unit(), account.status(), account.roles());
            accounts.put(generatedId, savedAccount);
            return generatedId;
        }

        @Override
        public void updateStatus(String accountId, AccountStatus status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findRoles(String accountId) {
            return accounts.containsKey(accountId) ? accounts.get(accountId).roles() : List.of();
        }

        @Override
        public void saveRoles(String accountId, List<Role> roles, String operatorId, LocalDateTime time) {
            savedRolesAccountId = accountId;
            savedRoles = roles;
        }

        @Override
        public void saveChangeLog(AccountChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeApplicationRepository implements ApplicationRepository {
        private final Map<String, AccountApplication> applications = new LinkedHashMap<>();
        private PageResult<AccountApplication> pendingPage = new PageResult<>(List.of(), 0, 1, 20);
        private IdentityType lastApproverIdentity;
        private SystemType lastSystem;
        private int lastPage;
        private int lastSize;
        private ApplicationStatus lastStatus;
        private String lastApproverId;
        private ApprovalRecord savedApproval;

        @Override
        public String save(AccountApplication application) {
            applications.put(application.applicationId(), application);
            return application.applicationId();
        }

        @Override
        public Optional<AccountApplication> findById(String applicationId) {
            return Optional.ofNullable(applications.get(applicationId));
        }

        @Override
        public PageResult<AccountApplication> findPending(IdentityType approverIdentity, SystemType system,
                                                          int page, int size) {
            lastApproverIdentity = approverIdentity;
            lastSystem = system;
            lastPage = page;
            lastSize = size;
            return pendingPage;
        }

        @Override
        public void updateStatus(String applicationId, ApplicationStatus status, String approverId,
                                 LocalDateTime time, String note) {
            lastStatus = status;
            lastApproverId = approverId;
        }

        @Override
        public void saveApproval(ApprovalRecord record) {
            savedApproval = record;
        }
    }

    private static final class FakeTeachingAdminRepository implements TeachingAdminRepository {
        private final Map<String, TeachingClass> classes = new LinkedHashMap<>();
        private final List<TeachingClass> savedClasses = new ArrayList<>();
        private final List<ClassMember> members = new ArrayList<>();
        private final Map<String, List<ClassMember>> classMemberMap = new HashMap<>();
        private final List<String> createdStudents = new ArrayList<>();
        private final Map<String, ImportRecord> importRecords = new LinkedHashMap<>();
        private final Map<String, String> studentClasses = new HashMap<>();
        private PageResult<TeachingClass> classPage = new PageResult<>(List.of(), 0, 1, 20);
        private String lastClassTerm;
        private String lastClassTeacher;

        @Override
        public String saveClass(TeachingClass teachingClass) {
            String classId = teachingClass.classId() == null
                    ? "class-" + (savedClasses.size() + 1) : teachingClass.classId();
            TeachingClass stored = new TeachingClass(classId, teachingClass.classCode(), teachingClass.name(),
                    teachingClass.teacherId(), teachingClass.term(), teachingClass.createdAt());
            savedClasses.add(stored);
            classes.put(classId, stored);
            return classId;
        }

        @Override
        public Optional<TeachingClass> findClass(String classId) {
            return Optional.ofNullable(classes.get(classId));
        }

        @Override
        public Optional<TeachingClass> findClassByCode(String classCode) {
            return classes.values().stream()
                    .filter(item -> item.classCode().equals(classCode))
                    .findFirst();
        }

        @Override
        public PageResult<TeachingClass> findClasses(String term, String teacherId, int page, int size) {
            lastClassTerm = term;
            lastClassTeacher = teacherId;
            return classPage;
        }

        @Override
        public void saveClassMember(String classId, String studentId, String studentNo, LocalDateTime joinedAt,
                                    String status) {
            members.add(new ClassMember(classId, studentId, studentNo, joinedAt, status));
            classMemberMap.computeIfAbsent(classId, key -> new ArrayList<>())
                    .add(new ClassMember(classId, studentId, studentNo, joinedAt, status));
            studentClasses.put(studentId, classId);
        }

        @Override
        public List<ClassMember> findClassMembers(String classId) {
            return classMemberMap.getOrDefault(classId, List.of());
        }

        @Override
        public void transferClass(String studentId, String fromClassId, String toClassId, LocalDateTime time) {
            studentClasses.put(studentId, toClassId);
            classMemberMap.computeIfAbsent(toClassId, key -> new ArrayList<>())
                    .add(new ClassMember(toClassId, studentId, null, time, "ACTIVE"));
        }

        @Override
        public String saveImportRecord(ImportRecord record) {
            String importId = record.importId() == null ? "imp-" + (importRecords.size() + 1) : record.importId();
            importRecords.put(importId, new ImportRecord(importId, record.operatorId(), record.importedAt(),
                    record.classCount(), record.studentCount(), record.failures()));
            return importId;
        }

        @Override
        public Optional<ImportRecord> findImportRecord(String importId) {
            return Optional.ofNullable(importRecords.get(importId));
        }

        @Override
        public Optional<TeachingClass> findClassByStudentId(String studentId) {
            String classId = studentClasses.get(studentId);
            return classId == null ? Optional.empty() : Optional.ofNullable(classes.get(classId));
        }

        @Override
        public Optional<Account> findByStudentNo(String studentNo) {
            return createdStudents.contains(studentNo)
                    ? Optional.of(new Account("stu-" + studentNo, studentNo, "pw", "student", null,
                    AccountStatus.ENABLED, List.of()))
                    : Optional.empty();
        }

        @Override
        public Optional<Account> findByTeacherNo(String teacherNo) {
            return Optional.empty();
        }

        @Override
        public StudentAccountResult createStudentAccount(String studentNo, String name, String classId) {
            createdStudents.add(studentNo);
            studentClasses.put("stu-" + studentNo, classId);
            return new StudentAccountResult("stu-" + studentNo, "init", null, true);
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

    private static final class FakeIdentityRule implements IdentityRule {
        private boolean enabled = true;
        private DataScope scope = DataScope.TEACHING_ACCOUNTS;
        private final Set<IdentityType> roles = Set.of(IdentityType.TEACHING_ADMIN);

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return roles.contains(identity);
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return scope;
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

    private static final class FakeAccountRule implements AccountRule {
        private final Map<ApprovalAction, ApplicationStatus> next = Map.of(
                ApprovalAction.APPROVE, ApplicationStatus.APPROVED,
                ApprovalAction.RETURN, ApplicationStatus.RETURNED,
                ApprovalAction.REJECT, ApplicationStatus.REJECTED);

        @Override
        public ApplicationStatus nextApplicationStatus(ApplicationStatus current, ApprovalAction action) {
            return next.get(action);
        }

        @Override
        public boolean atLeastOneRole(List<Role> roles, Role removed) {
            return true;
        }

        @Override
        public boolean canModifyOtherRole(String operatorId, String targetAccountId) {
            return true;
        }
    }

    private static final class FakeTeachingAdminRule implements TeachingAdminRule {
        private final Map<String, String> teacherIds = new HashMap<>();
        private boolean rejectBlankStudentNo;

        @Override
        public String classAction(String classCode, List<TeachingClass> existing) {
            return existing.isEmpty() ? "CREATE" : "UPDATE";
        }

        @Override
        public boolean uniqueStudentNo(String studentNo, List<Account> existing) {
            return existing.stream().noneMatch(item -> item.account().equals(studentNo));
        }

        @Override
        public String studentClass(String studentId, String classId, List<ClassMember> members) {
            return members.stream().anyMatch(item -> item.studentId().equals(studentId)) ? "STAY" : "JOIN";
        }

        @Override
        public Optional<String> matchTeacher(String teacherNo, List<Account> teachers) {
            return Optional.ofNullable(teacherIds.get(teacherNo));
        }

        @Override
        public ImportRow checkRow(ImportRow row) {
            if (rejectBlankStudentNo && (row.studentNo() == null || row.studentNo().isBlank())) {
                return new ImportRow(row.rowNo(), row.classNo(), row.studentNo(), row.name(), row.teacherNo(),
                        List.of("缺少学号"));
            }
            return row;
        }

        @Override
        public Account studentAccount(String studentNo, String name) {
            return new Account(null, studentNo, "pw", name, null, AccountStatus.ENABLED, List.of());
        }
    }

    private static final class FakeNotificationService implements NotificationService {
        private final List<String> inAppReceivers = new ArrayList<>();

        @Override
        public String sendInApp(String receiverId, String content, String type) {
            inAppReceivers.add(receiverId);
            return "n-" + inAppReceivers.size();
        }

        @Override
        public boolean sendMail(String receiver, String subject, String content) {
            return true;
        }
    }
}
