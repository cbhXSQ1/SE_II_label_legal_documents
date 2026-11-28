package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.ReadingRecord;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.publicquery.GuideListResult;
import com.seii.legal.function.dto.publicquery.UserListResult;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicQueryAppServiceTest {

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeGuideRepository guides = new FakeGuideRepository();
    private final FakeIdentityRule identities = new FakeIdentityRule(true, Set.of(IdentityType.ANNOTATOR));
    private final PublicQueryAppService service = new PublicQueryAppService(sessions, accounts, guides, identities);

    PublicQueryAppServiceTest() {
        sessions.saved.put("cred-1", new LoginUser("manager", SystemType.ANNOTATION,
                IdentityType.ANNOTATOR, "cred-1", LocalDateTime.now().plusHours(1)));
        accounts.accounts.add(account("manager", "manager", "Manager", AccountStatus.ENABLED,
                new Role("ANNOTATOR", "标注员", SystemType.ANNOTATION)));
        accounts.accounts.add(account("u1", "alice", "Alice", AccountStatus.ENABLED,
                new Role("ANNOTATOR", "标注员", SystemType.ANNOTATION)));
        accounts.accounts.add(account("u2", "bob", "Bob", AccountStatus.DISABLED,
                new Role("ANNOTATOR", "标注员", SystemType.ANNOTATION)));
    }

    @Test
    void listUsersFiltersDisabledAccountsAndHidesPassword() {
        AppResult<UserListResult> result = service.listUsers("cred-1", "ali", IdentityType.ANNOTATOR, 1, 20);

        assertTrue(result.success());
        assertEquals(1, result.data().users().size());
        assertEquals("u1", result.data().users().get(0).id());
        assertEquals("Alice", result.data().users().get(0).name());
        assertEquals(IdentityType.ANNOTATOR, result.data().users().get(0).identity());
        assertEquals(1, result.data().total());
        assertEquals(1, result.data().page());
        assertEquals(20, result.data().size());
        assertEquals("ali", accounts.lastKeyword);
        assertEquals(IdentityType.ANNOTATOR, accounts.lastIdentity);
    }

    @Test
    void listUsersWithoutIdentityUsesUnfilteredSearch() {
        AppResult<UserListResult> result = service.listUsers("cred-1", null, null, 1, 10);

        assertTrue(result.success());
        assertEquals(2, result.data().users().size());
        assertNull(accounts.lastIdentity);
        assertEquals(1, accounts.lastPage);
        assertEquals(10, accounts.lastSize);
    }

    @Test
    void listUsersRejectsMissingSession() {
        AppResult<UserListResult> result = service.listUsers("bad", null, null, 1, 20);

        assertFalse(result.success());
        assertEquals("登录已失效，请重新登录", result.message());
    }

    @Test
    void listUsersRejectsDisabledCurrentAccount() {
        identities.enabled = false;

        AppResult<UserListResult> result = service.listUsers("cred-1", null, null, 1, 20);

        assertFalse(result.success());
        assertEquals("账号已停用，请联系管理员", result.message());
    }

    @Test
    void listGuidesMapsAvailableVersions() {
        guides.available.add(new GuideVersion("v1.2", "全部", LocalDateTime.of(2026, 11, 27, 10, 30),
                "admin", "AVAILABLE", List.of(), List.of(), null, "f1"));

        AppResult<GuideListResult> result = service.listGuides("cred-1");

        assertTrue(result.success());
        assertEquals(1, result.data().versions().size());
        assertEquals("v1.2", result.data().versions().get(0).version());
        assertEquals("admin", result.data().versions().get(0).operator());
        assertEquals("全部", result.data().versions().get(0).scope());
        assertEquals(LocalDateTime.of(2026, 11, 27, 10, 30), result.data().versions().get(0).importedAt());
    }

    @Test
    void listGuidesRejectsMissingSession() {
        AppResult<GuideListResult> result = service.listGuides("bad");

        assertFalse(result.success());
        assertEquals("登录已失效，请重新登录", result.message());
    }

    private static Account account(String id, String login, String name, AccountStatus status, Role role) {
        return new Account(id, login, "secret", name, "课程组", status, List.of(role));
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
        private final List<Account> accounts = new java.util.ArrayList<>();
        private String lastKeyword;
        private IdentityType lastIdentity;
        private int lastPage;
        private int lastSize;

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
            lastKeyword = keyword;
            lastIdentity = null;
            lastPage = page;
            lastSize = size;
            List<Account> matched = accounts.stream()
                    .filter(a -> matchesKeyword(a, keyword))
                    .toList();
            return page(matched, page, size);
        }

        @Override
        public PageResult<Account> search(String keyword, IdentityType identity, int page, int size) {
            lastKeyword = keyword;
            lastIdentity = identity;
            lastPage = page;
            lastSize = size;
            List<Account> matched = accounts.stream()
                    .filter(a -> matchesKeyword(a, keyword))
                    .filter(a -> identity == null || a.roles().stream().anyMatch(r -> r.code().equals(identity.name())))
                    .toList();
            return page(matched, page, size);
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

        private static boolean matchesKeyword(Account account, String keyword) {
            return keyword == null
                    || account.account().contains(keyword)
                    || account.name().contains(keyword);
        }

        private static PageResult<Account> page(List<Account> source, int page, int size) {
            int from = Math.min(source.size(), (page - 1) * size);
            int to = Math.min(source.size(), from + size);
            return new PageResult<>(source.subList(from, to), source.size(), page, size);
        }
    }

    private static final class FakeGuideRepository implements GuideRepository {
        private final List<GuideVersion> available = new java.util.ArrayList<>();

        @Override
        public Optional<GuideVersion> findVersion(String version) {
            return available.stream().filter(v -> v.version().equals(version)).findFirst();
        }

        @Override
        public List<GuideVersion> findAvailable() {
            return available;
        }

        @Override
        public String saveVersion(GuideVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveReference(String objectType, String objectId, String version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findReference(String objectType, String objectId) {
            throw new UnsupportedOperationException();
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
