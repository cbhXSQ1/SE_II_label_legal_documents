package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
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
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.function.dto.TeachingAdminDtos;
import com.seii.legal.rule.identity.AccountRule;
import com.seii.legal.rule.teaching.TeachingAdminRule;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 教学系统管理员功能层服务（TA-01…06）。
 */
@Service
@RequiredArgsConstructor
public class TeachingAdminAppService {

    private static final Set<IdentityType> TEACHING_ADMIN_ONLY = Set.of(IdentityType.TEACHING_ADMIN);

    private final AuthActionTemplate authTemplate;
    private final TeachingAdminRepository teachingAdminRepository;
    private final AccountRepository accountRepository;
    private final ApplicationRepository applicationRepository;
    private final FileRepository fileRepository;
    private final TeachingAdminRule teachingAdminRule;
    private final AccountRule accountRule;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AppResult<TeachingAdminDtos.ImportRosterResponse> importRoster(
            String credential, TeachingAdminDtos.ImportRosterRequest request) {
        return authTemplate.execute(credential, TEACHING_ADMIN_ONLY, null, null, "仅教学系统管理员可导入班级名单", ctx -> {
            StoredFile file = fileRepository.find(request.fileId())
                    .orElseThrow(() -> new BusinessException("名单文件不存在"));
            List<ImportRow> parsedRows = parseRosterRows(file.content());

            List<TeachingClass> existingClasses = new ArrayList<>(
                    teachingAdminRepository.findClasses(null, null, 1, 500).records());
            List<Account> teachers = accountRepository.search(null, IdentityType.TEACHER, 1, 500).records();
            List<Account> existingStudents = new ArrayList<>(
                    accountRepository.search(null, IdentityType.STUDENT, 1, 1000).records());

            Set<String> touchedClassCodes = new HashSet<>();
            List<String> duplicateStudentNos = new ArrayList<>();
            List<Integer> missingRows = new ArrayList<>();
            List<String> unmatchedTeachers = new ArrayList<>();
            List<ImportRow> failedRows = new ArrayList<>();
            Map<String, String> classIdByCode = new HashMap<>();
            for (TeachingClass tc : existingClasses) {
                classIdByCode.put(tc.classCode(), tc.classId());
            }

            LocalDateTime now = TimeFormats.now();
            int successStudents = 0;

            for (ImportRow rawRow : parsedRows) {
                ImportRow checked = teachingAdminRule.checkRow(rawRow);
                if (!checked.valid()) {
                    missingRows.add(checked.rowNumber());
                    failedRows.add(checked);
                    continue;
                }

                Optional<String> teacherIdOpt = teachingAdminRule.matchTeacher(checked.teacherNo(), teachers);
                if (teacherIdOpt.isEmpty()) {
                    unmatchedTeachers.add(checked.classCode() + ":" + checked.teacherNo());
                    failedRows.add(new ImportRow(
                            checked.rowNumber(), checked.classCode(), checked.className(), checked.term(),
                            checked.teacherNo(), checked.studentNo(), checked.studentName(), false, "未匹配到教师工号"));
                    continue;
                }

                String classId = classIdByCode.get(checked.classCode());
                if (classId == null) {
                    teachingAdminRule.classAction(checked.classCode(), existingClasses);
                    TeachingClass newClass = new TeachingClass(
                            null, checked.classCode(), checked.className(), checked.term(), teacherIdOpt.get(), 0);
                    classId = teachingAdminRepository.saveClass(newClass);
                    classIdByCode.put(checked.classCode(), classId);
                }
                touchedClassCodes.add(checked.classCode());

                boolean uniqueNo = teachingAdminRule.uniqueStudentNo(checked.studentNo(), existingStudents);
                if (!uniqueNo) {
                    duplicateStudentNos.add(checked.studentNo());
                }

                StudentAccountResult accRes = teachingAdminRepository.createStudentAccount(
                        checked.studentNo(), checked.studentName(), classId);
                String studentId = accRes.accountId();

                Optional<TeachingClass> prevClass = teachingAdminRepository.findClassByStudentId(studentId);
                if (prevClass.isPresent() && !prevClass.get().classId().equals(classId)) {
                    List<ClassMember> members = teachingAdminRepository.findClassMembers(prevClass.get().classId());
                    teachingAdminRule.studentClass(studentId, classId, members);
                    teachingAdminRepository.transferClass(studentId, prevClass.get().classId(), classId, now);
                } else {
                    teachingAdminRepository.saveClassMember(classId, studentId, checked.studentNo(), now, "ACTIVE");
                }
                successStudents++;
            }

            ImportRecord record = new ImportRecord(
                    null, ctx.user().accountId(), now, request.fileId(),
                    touchedClassCodes.size(), successStudents, failedRows, request.fileId());
            String importId = teachingAdminRepository.saveImportRecord(record);

            return AppResult.ok(new TeachingAdminDtos.ImportRosterResponse(
                    importId, touchedClassCodes.size(), successStudents,
                    duplicateStudentNos, missingRows, unmatchedTeachers));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TeachingAdminDtos.ImportResultResponse> importResult(String credential, String importId) {
        return authTemplate.execute(credential, TEACHING_ADMIN_ONLY, null, null, "仅教学系统管理员可查看导入结果", ctx -> {
            ImportRecord rec = teachingAdminRepository.findImportRecord(importId)
                    .orElseThrow(() -> new BusinessException("导入记录不存在"));
            List<TeachingAdminDtos.FailedRowItem> failed = rec.failedRows() == null
                    ? List.of()
                    : rec.failedRows().stream()
                    .map(r -> new TeachingAdminDtos.FailedRowItem(r.rowNumber(), r.failureReason()))
                    .toList();
            String url = rec.resultFileId() == null ? "" : "/app/file/" + rec.resultFileId();
            return AppResult.ok(new TeachingAdminDtos.ImportResultResponse(
                    rec.successClasses(), rec.successStudents(), failed, url));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<TeachingAdminDtos.ClassItem>> listClasses(
            String credential, String term, String teacherId, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHING_ADMIN_ONLY, null, null, "仅教学系统管理员可查看班级列表", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            PageResult<TeachingClass> raw = teachingAdminRepository.findClasses(term, teacherId, p, s);
            List<TeachingAdminDtos.ClassItem> items = raw.records().stream()
                    .map(c -> new TeachingAdminDtos.ClassItem(
                            c.classId(), c.classCode(), c.name(), c.term(), resolveUserName(c.teacherId()), c.studentCount()))
                    .toList();
            return AppResult.ok(new PageResult<>(items, raw.total(), raw.page(), raw.size()));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<TeachingAdminDtos.TeachingAccountItem>> listAccounts(
            String credential, String classId, AccountStatus status, String keyword, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHING_ADMIN_ONLY, null, classId, "仅教学系统管理员可查看教学账号", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            Set<String> classMemberIds = null;
            String fixedClassName = null;
            if (classId != null && !classId.isBlank()) {
                classMemberIds = new HashSet<>();
                for (ClassMember cm : teachingAdminRepository.findClassMembers(classId)) {
                    classMemberIds.add(cm.studentId());
                }
                fixedClassName = teachingAdminRepository.findClass(classId).map(TeachingClass::name).orElse("");
            }

            PageResult<Account> raw = accountRepository.search(keyword, p, s);
            List<TeachingAdminDtos.TeachingAccountItem> items = new ArrayList<>();
            for (Account acc : raw.records()) {
                if (status != null && acc.status() != status) {
                    continue;
                }
                String teachingRole = acc.roles() == null ? null : acc.roles().stream()
                        .filter(r -> r.system() == SystemType.TEACHING)
                        .map(Role::code)
                        .findFirst()
                        .orElse(null);
                if (teachingRole == null) {
                    continue;
                }
                if (classMemberIds != null && !classMemberIds.contains(acc.accountId())) {
                    continue;
                }
                String clsName = fixedClassName != null
                        ? fixedClassName
                        : teachingAdminRepository.findClassByStudentId(acc.accountId()).map(TeachingClass::name).orElse("");
                items.add(new TeachingAdminDtos.TeachingAccountItem(
                        acc.accountId(), acc.account(), acc.name(), teachingRole, acc.status(), clsName));
            }
            return AppResult.ok(new PageResult<>(items, items.size(), p, s));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<AccountApplication>> listApplications(
            String credential, String status, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHING_ADMIN_ONLY, null, null, "仅教学系统管理员可查看教学账号申请", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            PageResult<AccountApplication> raw = applicationRepository.findPending(
                    IdentityType.TEACHING_ADMIN, SystemType.TEACHING, p, s);
            List<AccountApplication> filtered = raw.records().stream()
                    .filter(a -> status == null || status.isBlank() || a.status().name().equalsIgnoreCase(status))
                    .toList();
            return AppResult.ok(new PageResult<>(filtered, raw.total(), raw.page(), raw.size()));
        });
    }

    @Transactional
    public AppResult<Void> reviewApplication(
            String credential, TeachingAdminDtos.ReviewTeachingApplicationRequest request) {
        return authTemplate.execute(credential, TEACHING_ADMIN_ONLY, null, null, "仅教学系统管理员可审批教学账号申请", ctx -> {
            AccountApplication app = applicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new BusinessException("账号申请不存在"));
            if (app.system() != SystemType.TEACHING) {
                return AppResult.fail("无权审批非教学系统的账号申请");
            }
            IdentityType grantedIdentity = request.identity() != null ? request.identity() : app.identity();
            if (grantedIdentity != IdentityType.TEACHER && grantedIdentity != IdentityType.STUDENT) {
                return AppResult.fail("教学系统仅支持审批教师或学生身份");
            }
            ApplicationStatus nextStatus = accountRule.nextApplicationStatus(app.status(), request.action());
            LocalDateTime now = TimeFormats.now();

            applicationRepository.updateStatus(app.applicationId(), nextStatus, ctx.user().accountId(), now, request.note());
            applicationRepository.saveApproval(new ApprovalRecord(
                    null, app.applicationId(), ctx.user().accountId(), now, nextStatus, request.note()));

            String receiverId = app.applicationId();
            if (nextStatus == ApplicationStatus.APPROVED) {
                Role role = new Role(grantedIdentity.name(), grantedIdentity.displayName(), SystemType.TEACHING);
                String loginCode = (grantedIdentity == IdentityType.STUDENT && app.studentNo() != null)
                        ? app.studentNo()
                        : (app.teacherNo() != null ? app.teacherNo() : app.name());
                String newAccountId = accountRepository.save(new Account(
                        null, loginCode, "123456", app.name(), app.unit(), AccountStatus.ENABLED, List.of(role)));
                accountRepository.saveRoles(newAccountId, List.of(role), ctx.user().accountId(), now);
                receiverId = newAccountId;
            }

            eventPublisher.publishEvent(new DomainNotificationEvent(
                    List.of(receiverId),
                    "TEACHING_APPROVAL",
                    "教学系统账号申请审批结果",
                    "您的教学系统账号申请结果：" + nextStatus.name(),
                    true
            ));
            return AppResult.ok(null);
        });
    }

    private List<ImportRow> parseRosterRows(byte[] content) {
        if (content == null || content.length == 0) {
            return List.of();
        }
        String text = new String(content, StandardCharsets.UTF_8);
        String[] lines = text.split("\\r?\\n");
        List<ImportRow> rows = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || (i == 0 && line.contains("班级编号"))) {
                continue;
            }
            String[] cols = line.split(",", -1);
            rows.add(new ImportRow(
                    i + 1,
                    col(cols, 0),
                    col(cols, 1),
                    col(cols, 2),
                    col(cols, 3),
                    col(cols, 4),
                    col(cols, 5),
                    true,
                    null
            ));
        }
        return rows;
    }

    private String col(String[] cols, int idx) {
        return idx < cols.length ? cols[idx].trim() : "";
    }

    private String resolveUserName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }
}
