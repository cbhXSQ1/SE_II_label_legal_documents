package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.exception.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.ApprovalRecord;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.AdminDtos;
import com.seii.legal.function.event.DomainNotificationEvent;
import com.seii.legal.function.support.AuthActionTemplate;
import com.seii.legal.function.support.FunctionHelpers;
import com.seii.legal.function.support.TimeFormats;
import com.seii.legal.rule.guide.GuideRule;
import com.seii.legal.rule.identity.AccountRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员端用例编排（ADMIN-01…07）。
 */
@Service
public class AdminAppService {

    private static final Set<IdentityType> ADMIN_ONLY = Set.of(IdentityType.ADMIN);

    private final AuthActionTemplate authTemplate;
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final GuideRepository guideRepository;
    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final AccountRule accountRule;
    private final GuideRule guideRule;
    private final ApplicationEventPublisher eventPublisher;

    public AdminAppService(
            AuthActionTemplate authTemplate,
            ApplicationRepository applicationRepository,
            AccountRepository accountRepository,
            GuideRepository guideRepository,
            FileRepository fileRepository,
            TaskRepository taskRepository,
            AccountRule accountRule,
            GuideRule guideRule,
            ApplicationEventPublisher eventPublisher) {
        this.authTemplate = authTemplate;
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.guideRepository = guideRepository;
        this.fileRepository = fileRepository;
        this.taskRepository = taskRepository;
        this.accountRule = accountRule;
        this.guideRule = guideRule;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<AdminDtos.ApplicationItem>> listApplications(
            String credential, String status, Integer page, Integer size) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可查看账号申请", ctx -> {
            int p = FunctionHelpers.normalizePage(page);
            int s = FunctionHelpers.normalizeSize(size);
            PageResult<AccountApplication> raw = applicationRepository.findPending(IdentityType.ADMIN, null, p, s);
            List<AdminDtos.ApplicationItem> items = raw.records().stream()
                    .filter(a -> status == null || status.isBlank() || a.status().name().equalsIgnoreCase(status))
                    .map(a -> new AdminDtos.ApplicationItem(
                            a.applicationId(),
                            a.name(),
                            a.unit(),
                            a.system(),
                            a.identity(),
                            a.identity() == IdentityType.STUDENT ? a.account() : null,
                            a.identity() == IdentityType.TEACHER ? a.account() : null,
                            a.note(),
                            TimeFormats.format(a.appliedAt()),
                            a.status()
                    ))
                    .toList();
            return AppResult.ok(new PageResult<>(items, raw.total(), raw.page(), raw.size()));
        });
    }

    @Transactional
    public AppResult<Void> reviewApplication(String credential, AdminDtos.ReviewApplicationRequest request) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可审批账号申请", ctx -> {
            if (request == null || request.applicationId() == null || request.applicationId().isBlank()) {
                return AppResult.fail("申请标识不能为空");
            }
            if (request.action() == null) {
                return AppResult.fail("请选择审批动作");
            }
            AccountApplication app = applicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new BusinessException("账号申请不存在"));
            ApplicationStatus nextStatus = accountRule.nextApplicationStatus(app.status(), request.action());
            LocalDateTime now = TimeFormats.now();

            applicationRepository.updateStatus(app.applicationId(), nextStatus, ctx.user().accountId(), now, request.note());
            applicationRepository.saveApproval(new ApprovalRecord(
                    null, app.applicationId(), nextStatus.name(), ctx.user().accountId(), now, request.note()));

            String receiverId = app.applicationId();
            if (nextStatus == ApplicationStatus.APPROVED) {
                List<IdentityType> granted = (request.roles() != null && !request.roles().isEmpty())
                        ? request.roles()
                        : List.of(app.identity());
                List<Role> roleEntities = granted.stream()
                        .map(r -> new Role(r.name(), FunctionHelpers.identityName(r), FunctionHelpers.defaultSystem(r)))
                        .toList();
                String loginCode = resolveAccountLogin(app);
                String newAccountId = accountRepository.save(new Account(
                        null, loginCode, "123456", app.name(), app.unit(), AccountStatus.ENABLED, roleEntities));
                accountRepository.saveRoles(newAccountId, roleEntities, ctx.user().accountId(), now);
                receiverId = newAccountId;
            }

            eventPublisher.publishEvent(new DomainNotificationEvent(
                    List.of(receiverId),
                    "APPROVAL_RESULT",
                    "账号申请审批结果通知",
                    "您的账号申请处理结果：" + nextStatus.name() + (request.note() == null ? "" : "，说明：" + request.note()),
                    true
            ));
            return AppResult.ok(null, List.of("查看待审批申请"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<AdminDtos.AccountItem>> listAccounts(
            String credential, String keyword, Integer page, Integer size) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可查看账号列表", ctx -> {
            int p = FunctionHelpers.normalizePage(page);
            int s = FunctionHelpers.normalizeSize(size);
            PageResult<Account> raw = accountRepository.search(keyword, p, s);
            List<AdminDtos.AccountItem> items = raw.records().stream()
                    .map(a -> new AdminDtos.AccountItem(
                            a.accountId(),
                            a.account(),
                            a.name(),
                            a.unit(),
                            a.roles() == null ? List.of() : a.roles().stream().map(Role::code).toList(),
                            a.status()
                    ))
                    .toList();
            return AppResult.ok(new PageResult<>(items, raw.total(), raw.page(), raw.size()));
        });
    }

    @Transactional
    public AppResult<Void> updateAccountRoles(String credential, AdminDtos.UpdateAccountRolesRequest request) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可修改账号角色与状态", ctx -> {
            if (request == null || request.accountId() == null || request.accountId().isBlank()) {
                return AppResult.fail("账号标识不能为空");
            }
            if (!accountRule.canModifyOtherRole(ctx.user().accountId(), request.accountId())) {
                return AppResult.fail("自己的角色由其他管理员修改");
            }
            Account target = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new BusinessException("账号不存在"));
            List<Role> beforeRoles = target.roles() == null ? List.of() : target.roles();
            List<Role> afterRoles = (request.roles() == null)
                    ? beforeRoles
                    : request.roles().stream()
                    .map(r -> new Role(r.name(), FunctionHelpers.identityName(r), FunctionHelpers.defaultSystem(r)))
                    .toList();
            if (afterRoles.isEmpty() || !accountRule.atLeastOneRole(afterRoles, null)) {
                return AppResult.fail("账号至少要有一个角色");
            }
            AccountStatus afterStatus = request.status() == null ? target.status() : request.status();
            LocalDateTime now = TimeFormats.now();

            accountRepository.saveRoles(target.accountId(), afterRoles, ctx.user().accountId(), now);
            if (afterStatus != target.status()) {
                accountRepository.updateStatus(target.accountId(), afterStatus, ctx.user().accountId(), now);
            }
            accountRepository.saveChangeLog(new AccountChangeLog(
                    target.accountId(), beforeRoles, afterRoles, target.status(), afterStatus,
                    ctx.user().accountId(), now));
            return AppResult.ok(null);
        });
    }

    @Transactional
    public AppResult<AdminDtos.ImportGuideResponse> importGuide(
            String credential, AdminDtos.ImportGuideRequest request) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可导入指南", ctx -> {
            if (request == null || request.version() == null || request.version().isBlank()) {
                return AppResult.fail("版本号不能为空");
            }
            if (request.scope() == null || request.scope().isBlank()) {
                return AppResult.fail("适用范围不能为空");
            }
            List<GuideVersion> existing = guideRepository.findAvailable();
            if (!guideRule.canSaveVersion(request.version(), existing)) {
                return AppResult.fail("版本号已存在");
            }

            List<String> nonProps;
            List<String> props;
            List<String> relations;
            List<String> diagramRules;
            if (request.content() != null) {
                nonProps = safeList(request.content().nonProps());
                props = safeList(request.content().props());
                relations = safeList(request.content().relations());
                diagramRules = safeList(request.content().diagramRules());
            } else if (request.fileId() != null && !request.fileId().isBlank()) {
                StoredFile file = fileRepository.find(request.fileId())
                        .orElseThrow(() -> new BusinessException("指南文件不存在"));
                GuideRule.GuideContent parsed = guideRule.readLabels(file.content(), file.format());
                nonProps = safeList(parsed.nonPropositionTypes());
                props = safeList(parsed.propositionTypes());
                relations = safeList(parsed.relationTypes());
                diagramRules = safeList(parsed.diagramRules());
            } else {
                return AppResult.fail("请上传指南文件或提供解析后的标签内容");
            }

            List<String> mergedTags = new ArrayList<>();
            mergedTags.addAll(nonProps);
            mergedTags.addAll(props);
            GuideVersion version = new GuideVersion(
                    request.version().trim(),
                    request.scope().trim(),
                    TimeFormats.now(),
                    ctx.user().accountId(),
                    "ACTIVE",
                    mergedTags,
                    relations,
                    String.join("\n", diagramRules),
                    request.fileId()
            );
            guideRule.ensureReadOnly(version);
            String savedVersion = guideRepository.saveVersion(version);
            return AppResult.ok(new AdminDtos.ImportGuideResponse(savedVersion), List.of("查看指南版本列表"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<AdminDtos.GuideListResponse> listGuides(String credential) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可查看指南版本管理", ctx -> {
            List<GuideVersion> list = guideRepository.findAvailable();
            List<AdminDtos.GuideVersionItem> items = list.stream()
                    .map(g -> new AdminDtos.GuideVersionItem(
                            g.version(),
                            TimeFormats.format(g.importedAt()),
                            resolveUserName(g.operatorId()),
                            g.scope(),
                            g.status()
                    ))
                    .toList();
            String latest = items.isEmpty() ? null : items.get(0).version();
            return AppResult.ok(new AdminDtos.GuideListResponse(items, latest));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<AdminDtos.OverviewResponse> overview(
            String credential, SystemType system, Integer page, Integer size) {
        return authTemplate.execute(credential, ADMIN_ONLY, null, null, "仅管理员可查看总体情况", ctx -> {
            int p = FunctionHelpers.normalizePage(page);
            int s = FunctionHelpers.normalizeSize(size);
            PageResult<AnnotationTask> raw = taskRepository.findAll(system, p, s);
            long totalConflicts = 0L;
            List<AdminDtos.OverviewTaskItem> taskItems = new ArrayList<>();
            for (AnnotationTask task : raw.records()) {
                TaskProgress progress = taskRepository.findProgress(task.taskId());
                int due = progress != null ? progress.expectedCount()
                        : (task.annotatorIds() == null ? 0 : task.annotatorIds().size());
                int submitted = progress != null ? progress.submittedCount() : 0;
                int unsubmitted = progress != null ? progress.unsubmittedCount() : Math.max(0, due - submitted);
                int conflictCount = progress != null ? progress.conflictCount() : 0;
                int confirmedCount = progress != null ? progress.confirmedCount() : 0;
                Double avgScore = progress != null ? progress.averageScore() : null;
                totalConflicts += conflictCount;
                taskItems.add(new AdminDtos.OverviewTaskItem(
                        task.taskId(),
                        task.name(),
                        resolveUserName(task.publisherId()),
                        TimeFormats.format(task.publishedAt()),
                        TimeFormats.format(task.deadline()),
                        due,
                        submitted,
                        unsubmitted,
                        conflictCount,
                        confirmedCount,
                        avgScore
                ));
            }
            return AppResult.ok(new AdminDtos.OverviewResponse(raw.total(), totalConflicts, taskItems));
        });
    }

    private String resolveAccountLogin(AccountApplication app) {
        if (app.account() != null && !app.account().isBlank()) {
            return app.account().trim();
        }
        return "u_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String resolveUserName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }

    private List<String> safeList(List<String> input) {
        return input == null ? List.of() : input;
    }
}
