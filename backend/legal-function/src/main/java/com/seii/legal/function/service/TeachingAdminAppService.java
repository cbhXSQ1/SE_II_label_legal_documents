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
import com.seii.legal.function.dto.teachingadmin.ImportFailure;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeachingAdminAppService {

    private static final String IMPORT_FORBIDDEN = "只有教学管理员可以导入班级和学生名单";
    private static final String CLASS_FORBIDDEN = "只有教学管理员可以查看班级";
    private static final String ACCOUNT_FORBIDDEN = "只有教学管理员可以查看教学账号";
    private static final String REVIEW_FORBIDDEN = "只有教学管理员可以审批教学账号申请";
    private static final String SCOPE_FORBIDDEN = "当前身份不能查看教学账号";

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final ApplicationRepository applicationRepository;
    private final TeachingAdminRepository teachingAdminRepository;
    private final FileRepository fileRepository;
    private final IdentityRule identityRule;
    private final AccountRule accountRule;
    private final TeachingAdminRule teachingAdminRule;
    private final NotificationService notificationService;

    public TeachingAdminAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            ApplicationRepository applicationRepository,
            TeachingAdminRepository teachingAdminRepository,
            FileRepository fileRepository,
            IdentityRule identityRule,
            AccountRule accountRule,
            TeachingAdminRule teachingAdminRule,
            NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.applicationRepository = applicationRepository;
        this.teachingAdminRepository = teachingAdminRepository;
        this.fileRepository = fileRepository;
        this.identityRule = identityRule;
        this.accountRule = accountRule;
        this.teachingAdminRule = teachingAdminRule;
        this.notificationService = notificationService;
    }

    @Transactional
    public AppResult<ImportResult> importClasses(String credential, ImportCommand command) {
        AuthOutcome auth = authorize(credential, IMPORT_FORBIDDEN, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        Optional<StoredFile> found = command == null || command.fileId() == null
                ? Optional.empty()
                : fileRepository.find(command.fileId());
        if (found.isEmpty()) {
            return AppResult.fail("导入文件不存在");
        }
        List<ParsedRow> rows = parseRows(found.get());
        if (rows.isEmpty()) {
            return AppResult.fail("导入文件格式不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Account> existingStudents = new ArrayList<>();
        List<String> duplicateStudentNos = new ArrayList<>();
        List<ImportFailure> missingRows = new ArrayList<>();
        List<String> unmatchedTeachers = new ArrayList<>();
        List<ImportRow> failures = new ArrayList<>();
        Set<String> importedClasses = new LinkedHashSet<>();
        int studentCount = 0;

        for (ParsedRow row : rows) {
            ImportRow checked = teachingAdminRule.checkRow(new ImportRow(
                    row.rowNo(), row.classNo(), row.studentNo(), row.name(), row.teacherNo(), List.of()));
            if (checked.errors() != null && !checked.errors().isEmpty()) {
                missingRows.add(new ImportFailure(row.rowNo(), String.join("；", checked.errors())));
                failures.add(checked);
                continue;
            }
            if (!teachingAdminRule.uniqueStudentNo(row.studentNo(), existingStudents)) {
                duplicateStudentNos.add(row.studentNo());
                failures.add(new ImportRow(row.rowNo(), row.classNo(), row.studentNo(), row.name(),
                        row.teacherNo(), List.of("学号重复")));
                continue;
            }

            Optional<String> teacherId = teachingAdminRule.matchTeacher(row.teacherNo(),
                    teachingAdminRepository.findByTeacherNo(row.teacherNo()).map(List::of).orElse(List.of()));
            if (teacherId.isEmpty() && row.teacherNo() != null && !unmatchedTeachers.contains(row.teacherNo())) {
                unmatchedTeachers.add(row.teacherNo());
            }

            Optional<TeachingClass> existingClass = teachingAdminRepository.findClassByCode(row.classNo());
            teachingAdminRule.classAction(row.classNo(), existingClass.map(List::of).orElse(List.of()));
            String classId;
            if (existingClass.isPresent()) {
                classId = existingClass.get().classId();
            } else {
                classId = teachingAdminRepository.saveClass(new TeachingClass(null, row.classNo(), row.classNo(),
                        teacherId.orElse(null), null, now));
            }
            importedClasses.add(row.classNo());

            Optional<Account> existingAccount = teachingAdminRepository.findByStudentNo(row.studentNo());
            String studentId;
            if (existingAccount.isPresent()) {
                studentId = existingAccount.get().accountId();
                existingStudents.add(existingAccount.get());
            } else {
                StudentAccountResult created = teachingAdminRepository.createStudentAccount(
                        row.studentNo(), row.name(), classId);
                studentId = created.accountId();
                existingStudents.add(new Account(studentId, row.studentNo(), null, row.name(), null,
                        AccountStatus.ENABLED, List.of()));
            }

            List<ClassMember> members = teachingAdminRepository.findClassMembers(classId);
            teachingAdminRule.studentClass(studentId, classId, members);
            Optional<TeachingClass> currentClass = teachingAdminRepository.findClassByStudentId(studentId);
            if (currentClass.isPresent() && !currentClass.get().classId().equals(classId)) {
                teachingAdminRepository.transferClass(studentId, currentClass.get().classId(), classId, now);
            } else {
                teachingAdminRepository.saveClassMember(classId, studentId, row.studentNo(), now, "ACTIVE");
            }
            studentCount++;
        }

        ImportRecord record = new ImportRecord(null, auth.user().accountId(), now,
                importedClasses.size(), studentCount, failures);
        String importId = teachingAdminRepository.saveImportRecord(record);
        ImportResult result = new ImportResult(importId, importedClasses.size(), studentCount,
                duplicateStudentNos, missingRows, unmatchedTeachers);
        return AppResult.ok(result, List.of("查看导入结果"));
    }

    @Transactional(readOnly = true)
    public AppResult<ImportResultView> importResult(String credential, String importId) {
        AuthOutcome auth = authorize(credential, CLASS_FORBIDDEN, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        Optional<ImportRecord> record = teachingAdminRepository.findImportRecord(importId);
        if (record.isEmpty()) {
            return AppResult.fail("导入记录不存在");
        }
        List<ImportFailure> failedRows = record.get().failures() == null ? List.of()
                : record.get().failures().stream()
                .map(row -> new ImportFailure(row.rowNo(), row.errors() == null ? "" : String.join("；", row.errors())))
                .toList();
        return AppResult.ok(new ImportResultView(record.get().classCount(), record.get().studentCount(),
                failedRows, null), List.of("查看班级"));
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<ClassView>> listClasses(String credential, ClassQuery query) {
        AuthOutcome auth = authorize(credential, CLASS_FORBIDDEN, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        int page = page(query == null ? null : query.page());
        int size = size(query == null ? null : query.size());
        String term = query == null ? null : query.term();
        String teacherId = query == null ? null : query.teacherId();
        PageResult<TeachingClass> source = teachingAdminRepository.findClasses(term, teacherId, page, size);
        List<ClassView> views = source.records().stream()
                .map(item -> new ClassView(item.classId(), item.classCode(), item.name(), item.term(),
                        teacherName(item.teacherId()),
                        teachingAdminRepository.findClassMembers(item.classId()).size()))
                .toList();
        return AppResult.ok(new PageResult<>(views, source.total(), source.page(), source.size()),
                List.of("查看班级账号"));
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<AccountView>> listAccounts(String credential, AccountQuery query) {
        String classId = query == null ? null : query.classId();
        AuthOutcome auth = authorize(credential, ACCOUNT_FORBIDDEN, classId);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        int page = page(query == null ? null : query.page());
        int size = size(query == null ? null : query.size());
        String keyword = query == null ? null : query.keyword();
        String status = query == null ? null : query.status();
        PageResult<Account> source = accountRepository.search(keyword, page, size);
        Set<String> allowedStudentIds = null;
        if (classId != null && !classId.isBlank()) {
            allowedStudentIds = teachingAdminRepository.findClassMembers(classId).stream()
                    .map(ClassMember::studentId)
                    .collect(Collectors.toSet());
        }
        Set<String> filterIds = allowedStudentIds;
        AccountStatus statusFilter = parseStatus(status);
        List<AccountView> views = source.records().stream()
                .filter(item -> filterIds == null || filterIds.contains(item.accountId()))
                .filter(item -> statusFilter == null || item.status() == statusFilter)
                .filter(item -> {
                    IdentityType identity = teachingIdentity(item);
                    return identity == IdentityType.TEACHER || identity == IdentityType.STUDENT;
                })
                .map(this::toAccountView)
                .toList();
        return AppResult.ok(new PageResult<>(views, views.size(), source.page(), source.size()),
                List.of("查看账号申请"));
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<AccountApplication>> listApplications(String credential, ApplicationQuery query) {
        AuthOutcome auth = authorize(credential, REVIEW_FORBIDDEN, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        int page = page(query == null ? null : query.page());
        int size = size(query == null ? null : query.size());
        PageResult<AccountApplication> result = applicationRepository.findPending(
                IdentityType.TEACHING_ADMIN, SystemType.TEACHING, page, size);
        return AppResult.ok(result, List.of("审批账号申请"));
    }

    @Transactional
    public AppResult<Void> reviewApplication(String credential, ReviewCommand command) {
        AuthOutcome auth = authorize(credential, REVIEW_FORBIDDEN, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        if (command == null || command.applicationId() == null) {
            return AppResult.fail("申请不存在");
        }
        Optional<AccountApplication> found = applicationRepository.findById(command.applicationId());
        if (found.isEmpty()) {
            return AppResult.fail("申请不存在");
        }
        AccountApplication application = found.get();
        if (application.system() != SystemType.TEACHING) {
            return AppResult.fail("只能审批教学系统账号申请");
        }
        if (application.status() != ApplicationStatus.PENDING) {
            return AppResult.fail("这条申请已经审批");
        }
        AccountRule.ApprovalAction action = parseAction(command.action());
        if (action == null) {
            return AppResult.fail("审批动作不合法");
        }
        IdentityType identity = command.identity() != null ? command.identity() : application.identity();
        if (action == AccountRule.ApprovalAction.APPROVE
                && identity != IdentityType.TEACHER && identity != IdentityType.STUDENT) {
            return AppResult.fail("教学账号身份只能是教师或学生");
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorId = auth.user().accountId();
        ApplicationStatus nextStatus = accountRule.nextApplicationStatus(application.status(), action);
        if (action == AccountRule.ApprovalAction.APPROVE) {
            List<Role> roles = List.of(new Role(identity.name(), identityName(identity), SystemType.TEACHING));
            String accountId = accountRepository.save(new Account(null, application.account(), application.password(),
                    application.name(), application.unit(), AccountStatus.ENABLED, roles));
            accountRepository.saveRoles(accountId, roles, operatorId, now);
            notifyApproved(accountId);
        }
        applicationRepository.updateStatus(application.applicationId(), nextStatus, operatorId, now, command.note());
        applicationRepository.saveApproval(new ApprovalRecord(null, application.applicationId(), action.name(),
                operatorId, now, command.note()));
        return AppResult.ok(null, List.of("查看教学账号"));
    }

    private void notifyApproved(String accountId) {
        try {
            notificationService.sendInApp(accountId, "您的教学系统账号申请已通过", "APPLICATION_APPROVED");
        } catch (RuntimeException ignored) {
        }
    }

    private AccountView toAccountView(Account account) {
        String className = teachingAdminRepository.findClassByStudentId(account.accountId())
                .map(TeachingClass::name)
                .orElse(null);
        return new AccountView(account.accountId(), account.account(), account.name(),
                teachingIdentity(account), account.status(), className);
    }

    private IdentityType teachingIdentity(Account account) {
        if (account.roles() == null) {
            return null;
        }
        return account.roles().stream()
                .filter(role -> role.system() == SystemType.TEACHING)
                .map(Role::code)
                .map(this::parseIdentity)
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
    }

    private IdentityType parseIdentity(String code) {
        if (code == null) {
            return null;
        }
        try {
            return IdentityType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AccountStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AccountStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AccountRule.ApprovalAction parseAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        try {
            return AccountRule.ApprovalAction.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String identityName(IdentityType identity) {
        return switch (identity) {
            case TEACHER -> "教师";
            case STUDENT -> "学生";
            default -> identity.name();
        };
    }

    private String teacherName(String teacherId) {
        if (teacherId == null || teacherId.isBlank()) {
            return null;
        }
        return accountRepository.findById(teacherId).map(Account::name).orElse(teacherId);
    }

    private AuthOutcome authorize(String credential, String forbidden, String classId) {
        LoginUser user = sessionRepository.findByCredential(credential)
                .orElseThrow(() -> new IllegalStateException("登录已失效，请重新登录"));
        Account account = accountRepository.findById(user.accountId())
                .orElseThrow(() -> new IllegalStateException("账号不存在"));
        if (!identityRule.isEnabled(account)) {
            return new AuthOutcome(false, "账号已停用，请联系管理员", user, account, null);
        }
        if (user.identity() != IdentityType.TEACHING_ADMIN
                || !identityRule.hasRole(account, IdentityType.TEACHING_ADMIN)) {
            return new AuthOutcome(false, forbidden, user, account, null);
        }
        DataScope scope = identityRule.resolveScope(user, null, classId);
        if (scope != DataScope.TEACHING_ACCOUNTS) {
            return new AuthOutcome(false, SCOPE_FORBIDDEN, user, account, scope);
        }
        return new AuthOutcome(true, null, user, account, scope);
    }

    private int page(Integer page) {
        return page == null || page <= 0 ? 1 : page;
    }

    private int size(Integer size) {
        return size == null || size <= 0 ? 20 : size;
    }

    private List<ParsedRow> parseRows(StoredFile file) {
        String content = file.content() == null ? "" : new String(file.content(), StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        List<ParsedRow> rows = new ArrayList<>();
        boolean headerChecked = false;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] cells = line.split(",", -1);
            if (!headerChecked) {
                headerChecked = true;
                if (isHeader(cells)) {
                    continue;
                }
            }
            rows.add(new ParsedRow(index + 1, cell(cells, 0), cell(cells, 1), cell(cells, 2), cell(cells, 3)));
        }
        return rows;
    }

    private boolean isHeader(String[] cells) {
        if (cells.length == 0) {
            return false;
        }
        String first = cells[0].trim();
        return first.equals("班级编号") || first.equalsIgnoreCase("classNo");
    }

    private String cell(String[] cells, int index) {
        if (index >= cells.length) {
            return null;
        }
        String value = cells[index].trim();
        return value.isEmpty() ? null : value;
    }

    private record ParsedRow(int rowNo, String classNo, String studentNo, String name, String teacherNo) {
    }

    private record AuthOutcome(boolean ok, String message, LoginUser user, Account account, DataScope scope) {
    }
}
