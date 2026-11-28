package com.seii.legal.function.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.AdminDtos;
import com.seii.legal.function.event.DomainNotificationEvent;
import com.seii.legal.function.event.NotificationEventListener;
import com.seii.legal.function.support.AuthActionTemplate;
import com.seii.legal.rule.guide.GuideRule;
import com.seii.legal.rule.identity.AccountRule;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.notification.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AdminAppServiceTest {

    private SessionRepository sessionRepository;
    private AccountRepository accountRepository;
    private ApplicationRepository applicationRepository;
    private GuideRepository guideRepository;
    private FileRepository fileRepository;
    private TaskRepository taskRepository;
    private IdentityRule identityRule;
    private AccountRule accountRule;
    private GuideRule guideRule;
    private ApplicationEventPublisher eventPublisher;
    private AdminAppService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        accountRepository = mock(AccountRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        guideRepository = mock(GuideRepository.class);
        fileRepository = mock(FileRepository.class);
        taskRepository = mock(TaskRepository.class);
        identityRule = mock(IdentityRule.class);
        accountRule = mock(AccountRule.class);
        guideRule = mock(GuideRule.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        AuthActionTemplate authTemplate = new AuthActionTemplate(sessionRepository, accountRepository, identityRule);
        service = new AdminAppService(
                authTemplate, applicationRepository, accountRepository, guideRepository,
                fileRepository, taskRepository, accountRule, guideRule, eventPublisher);
    }

    @Test
    void reviewApplicationApproveCreatesAccountAndPublishesNotification() {
        stubAdminSession();
        AccountApplication app = new AccountApplication(
                "app-1", "20260001", null, "张三", "法学院",
                SystemType.TEACHING, IdentityType.STUDENT, ApplicationStatus.PENDING,
                LocalDateTime.now(), null, null, "申请学习");
        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        when(accountRule.nextApplicationStatus(ApplicationStatus.PENDING, AccountRule.ApprovalAction.APPROVE))
                .thenReturn(ApplicationStatus.APPROVED);
        when(accountRepository.save(any(Account.class))).thenReturn("acc-new");

        AppResult<Void> res = service.reviewApplication(
                "cred-admin",
                new AdminDtos.ReviewApplicationRequest("app-1", AccountRule.ApprovalAction.APPROVE, null, "同意"));

        assertTrue(res.success());
        verify(accountRepository).saveRoles(eq("acc-new"), any(), eq("u1"), any());
        ArgumentCaptor<DomainNotificationEvent> captor = ArgumentCaptor.forClass(DomainNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(List.of("acc-new"), captor.getValue().receiverIds());
        assertTrue(captor.getValue().sendMail());
    }

    @Test
    void updateAccountRolesRejectsSelfModification() {
        stubAdminSession();
        when(accountRule.canModifyOtherRole("u1", "u1")).thenReturn(false);

        AppResult<Void> res = service.updateAccountRoles(
                "cred-admin",
                new AdminDtos.UpdateAccountRolesRequest("u1", List.of(IdentityType.ADMIN), AccountStatus.ENABLED, "自改"));

        assertFalse(res.success());
        assertEquals("自己的角色由其他管理员修改", res.message());
    }

    @Test
    void notificationListenerSwallowsMailFailureAndKeepsInApp() {
        NotificationService notificationService = mock(NotificationService.class);
        when(notificationService.sendMail(any(), any(), any())).thenThrow(new RuntimeException("SMTP down"));
        NotificationEventListener listener = new NotificationEventListener(notificationService);

        listener.onDomainNotification(new DomainNotificationEvent(
                List.of("u2"), "APPROVAL_RESULT", "审批结果", "已通过", true));

        verify(notificationService).sendInApp("u2", "已通过", "APPROVAL_RESULT");
        verify(notificationService).sendMail("u2", "审批结果", "已通过");
    }

    private void stubAdminSession() {
        LoginUser admin = new LoginUser("u1", SystemType.ADMIN, IdentityType.ADMIN, "cred-admin", LocalDateTime.now().plusHours(2));
        Account adminAcc = new Account("u1", "admin", "123456", "管理员", "系统管理组", AccountStatus.ENABLED,
                List.of(new Role("ADMIN", "系统管理员", SystemType.ADMIN)));
        when(sessionRepository.findByCredential("cred-admin")).thenReturn(Optional.of(admin));
        when(accountRepository.findById("u1")).thenReturn(Optional.of(adminAcc));
        when(identityRule.isEnabled(adminAcc)).thenReturn(true);
        when(identityRule.hasRole(adminAcc, IdentityType.ADMIN)).thenReturn(true);
        when(identityRule.resolveScope(admin, null, null)).thenReturn(DataScope.ALL_TASKS);
    }
}
