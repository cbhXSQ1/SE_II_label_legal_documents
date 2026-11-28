package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
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
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.ApplyCommand;
import com.seii.legal.function.dto.ApplyResult;
import com.seii.legal.function.dto.LoginCommand;
import com.seii.legal.function.dto.OptionsView;
import com.seii.legal.function.dto.SelectCommand;
import com.seii.legal.function.dto.SessionView;
import com.seii.legal.rule.identity.IdentityRule;
import java.time.LocalDateTime;
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

class AuthAppServiceTest {

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeApplicationRepository applications = new FakeApplicationRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository(account(
            "u1", "alice", "pw", AccountStatus.ENABLED,
            List.of(new Role("ANNOTATOR", "标注员", SystemType.ANNOTATION))));
    private final FakeIdentityRule identities = new FakeIdentityRule(true,
            List.of(new IdentityRule.IdentityOption(SystemType.ANNOTATION, IdentityType.ANNOTATOR, "标注")),
            Set.of(IdentityType.ANNOTATOR));
    private final AuthAppService service = new AuthAppService(sessions, accounts, applications, identities);

    @Test
    void loginReturnsSingleIdentitySession() {
        AppResult<SessionView> result = service.login(new LoginCommand("alice", "pw"));

        assertTrue(result.success());
        SessionView view = result.data();
        assertEquals("u1", view.accountId());
        assertEquals("Alice", view.name());
        assertTrue(view.single());
        assertEquals(SystemType.ANNOTATION, view.system());
        assertEquals(IdentityType.ANNOTATOR, view.identity());
        assertNotNull(view.credential());
        assertEquals(1, view.options().size());
        assertTrue(sessions.saved.containsKey(view.credential()));
    }

    @Test
    void loginRejectsWrongPassword() {
        AppResult<SessionView> result = service.login(new LoginCommand("alice", "bad"));

        assertFalse(result.success());
        assertEquals("账号或密码错误", result.message());
        assertTrue(sessions.saved.isEmpty());
    }

    @Test
    void loginRejectsDisabledAccount() {
        identities.enabled = false;

        AppResult<SessionView> result = service.login(new LoginCommand("alice", "pw"));

        assertFalse(result.success());
        assertEquals("账号已停用，请联系管理员", result.message());
    }

    @Test
    void loginReturnsOptionsWhenMultipleIdentities() {
        identities.options = List.of(
                new IdentityRule.IdentityOption(SystemType.ANNOTATION, IdentityType.ANNOTATOR, "标注"),
                new IdentityRule.IdentityOption(SystemType.TEACHING, IdentityType.STUDENT, "学生"));

        AppResult<SessionView> result = service.login(new LoginCommand("alice", "pw"));

        assertTrue(result.success());
        SessionView view = result.data();
        assertFalse(view.single());
        assertNull(view.system());
        assertNull(view.identity());
        assertEquals(2, view.options().size());
        assertNotNull(view.credential());
        LoginUser saved = sessions.saved.get(view.credential());
        assertNotNull(saved);
        assertNull(saved.system());
        assertNull(saved.identity());
    }

    @Test
    void selectUpdatesSessionIdentity() {
        LoginUser existing = new LoginUser("u1", null, null, "cred-1", LocalDateTime.now().plusHours(1));
        sessions.saved.put("cred-1", existing);

        AppResult<SessionView> result = service.select("cred-1",
                new SelectCommand(SystemType.ANNOTATION, IdentityType.ANNOTATOR));

        assertTrue(result.success());
        assertEquals(SystemType.ANNOTATION, result.data().system());
        assertEquals(IdentityType.ANNOTATOR, result.data().identity());
        assertEquals(IdentityType.ANNOTATOR, sessions.saved.get("cred-1").identity());
    }

    @Test
    void selectRejectsIdentityWithoutRole() {
        sessions.saved.put("cred-1",
                new LoginUser("u1", null, null, "cred-1", LocalDateTime.now().plusHours(1)));

        AppResult<SessionView> result = service.select("cred-1",
                new SelectCommand(SystemType.TEACHING, IdentityType.STUDENT));

        assertFalse(result.success());
        assertEquals("账号没有该身份", result.message());
    }

    @Test
    void logoutDeletesCredential() {
        sessions.saved.put("cred-1",
                new LoginUser("u1", SystemType.ANNOTATION, IdentityType.ANNOTATOR, "cred-1",
                        LocalDateTime.now().plusHours(1)));

        AppResult<Void> result = service.logout("cred-1");

        assertTrue(result.success());
        assertTrue(sessions.saved.isEmpty());
    }

    @Test
    void applySavesApplication() {
        AppResult<ApplyResult> result = service.apply(new ApplyCommand(
                "张老师", "课程组", SystemType.TEACHING, IdentityType.TEACHER, null, "T001", "申请教师账号"));

        assertTrue(result.success());
        assertNotNull(result.data().applicationId());
        assertEquals("张老师", applications.saved.name());
        assertEquals(SystemType.TEACHING, applications.saved.system());
        assertEquals(IdentityType.TEACHER, applications.saved.identity());
    }

    @Test
    void optionsReturnsCurrentAndAvailable() {
        sessions.saved.put("cred-1",
                new LoginUser("u1", SystemType.ANNOTATION, IdentityType.ANNOTATOR, "cred-1",
                        LocalDateTime.now().plusHours(1)));

        AppResult<OptionsView> result = service.options("cred-1");

        assertTrue(result.success());
        assertEquals("Alice", result.data().name());
        assertEquals(SystemType.ANNOTATION, result.data().currentSystem());
        assertEquals(IdentityType.ANNOTATOR, result.data().currentIdentity());
        assertEquals(1, result.data().options().size());
    }

    private static Account account(String id, String login, String password, AccountStatus status, List<Role> roles) {
        return new Account(id, login, password, "Alice", "课程组", status, roles);
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, LoginUser> saved = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            String credential = user.credential() == null || user.credential().isBlank()
                    ? "cred-" + (saved.size() + 1) : user.credential();
            saved.put(credential, new LoginUser(user.accountId(), user.system(), user.identity(),
                    credential, user.expiresAt()));
            return credential;
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

    private static final class FakeApplicationRepository implements ApplicationRepository {
        private AccountApplication saved;

        @Override
        public String save(AccountApplication application) {
            saved = application;
            return "app-1";
        }

        @Override
        public Optional<AccountApplication> findById(String applicationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AccountApplication> findPending(IdentityType approverIdentity, SystemType system, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(String applicationId, com.seii.legal.common.enums.ApplicationStatus status,
                                 String approverId, LocalDateTime time, String note) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveApproval(ApprovalRecord record) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        private final Account account;

        private FakeAccountRepository(Account account) {
            this.account = account;
        }

        @Override
        public Optional<Account> findById(String accountId) {
            return account.accountId().equals(accountId) ? Optional.of(account) : Optional.empty();
        }

        @Override
        public Optional<Account> findByAccount(String login) {
            return account.account().equals(login) ? Optional.of(account) : Optional.empty();
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
            return account.roles();
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
        private boolean enabled;
        private List<IdentityOption> options;
        private final Set<IdentityType> roles;

        private FakeIdentityRule(boolean enabled, List<IdentityOption> options, Set<IdentityType> roles) {
            this.enabled = enabled;
            this.options = options;
            this.roles = roles;
        }

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
            return enabled;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            return options;
        }
    }
}
