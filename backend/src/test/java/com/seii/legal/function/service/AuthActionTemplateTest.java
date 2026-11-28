package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.AuthException;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.rule.identity.IdentityRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthActionTemplateTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private IdentityRule identityRule;

    private AuthActionTemplate template;

    @BeforeEach
    void setUp() {
        template = new AuthActionTemplate(sessionRepository, accountRepository, identityRule);
    }

    @Test
    void rejectsExpiredCredential() {
        when(sessionRepository.findByCredential("bad-token")).thenReturn(Optional.empty());
        AuthException ex = assertThrows(AuthException.class, () ->
                template.execute("bad-token", Set.of(IdentityType.ARBITER), "T1", null, "无权", ctx -> AppResult.ok("x")));
        assertEquals("登录已失效，请重新登录", ex.getMessage());
    }

    @Test
    void rejectsDisabledAccount() {
        LoginUser user = new LoginUser("u6", SystemType.ANNOTATION, IdentityType.ARBITER, "tok", LocalDateTime.now().plusHours(1));
        Account disabled = new Account("u6", "wang", "123456", "王仲裁", "课程组", AccountStatus.DISABLED, List.of());
        when(sessionRepository.findByCredential("tok")).thenReturn(Optional.of(user));
        when(accountRepository.findById("u6")).thenReturn(Optional.of(disabled));
        when(identityRule.isEnabled(disabled)).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () ->
                template.execute("tok", Set.of(IdentityType.ARBITER), "T1", null, "无权", ctx -> AppResult.ok("x")));
        assertEquals("账号已停用，请联系管理员", ex.getMessage());
    }

    @Test
    void rejectsWrongIdentityAndAllowsAuthorizedIdentity() {
        LoginUser user = new LoginUser("u6", SystemType.ANNOTATION, IdentityType.ARBITER, "tok", LocalDateTime.now().plusHours(1));
        Account active = new Account("u6", "wang", "123456", "王仲裁", "课程组", AccountStatus.ENABLED,
                List.of(new Role("ARBITER", "仲裁员", SystemType.ANNOTATION)));
        when(sessionRepository.findByCredential("tok")).thenReturn(Optional.of(user));
        when(accountRepository.findById("u6")).thenReturn(Optional.of(active));
        when(identityRule.isEnabled(active)).thenReturn(true);

        AppResult<String> denied = template.execute("tok", Set.of(IdentityType.ADMIN), null, null, "仅管理员可操作", ctx -> AppResult.ok("ok"));
        assertFalse(denied.success());
        assertEquals("仅管理员可操作", denied.message());

        when(identityRule.hasRole(active, IdentityType.ARBITER)).thenReturn(true);
        when(identityRule.resolveScope(user, "T1", null)).thenReturn(DataScope.TASK_SUBMISSIONS);

        AppResult<String> allowed = template.execute("tok", Set.of(IdentityType.ARBITER), "T1", null, "无权",
                ctx -> AppResult.ok(ctx.scope().name()));
        assertTrue(allowed.success());
        assertEquals("TASK_SUBMISSIONS", allowed.data());
    }
}
