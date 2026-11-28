package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.publicquery.GuideListResult;
import com.seii.legal.function.dto.publicquery.GuideVersionView;
import com.seii.legal.function.dto.publicquery.UserListResult;
import com.seii.legal.function.dto.publicquery.UserView;
import com.seii.legal.rule.identity.IdentityRule;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicQueryAppService {

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final GuideRepository guideRepository;
    private final IdentityRule identityRule;

    public PublicQueryAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            GuideRepository guideRepository,
            IdentityRule identityRule) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.guideRepository = guideRepository;
        this.identityRule = identityRule;
    }

    @Transactional(readOnly = true)
    public AppResult<UserListResult> listUsers(
            String credential,
            String keyword,
            IdentityType identity,
            int page,
            int size) {
        Auth auth = authenticate(credential);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        int safePage = page <= 0 ? 1 : page;
        int safeSize = size <= 0 ? 20 : size;
        PageResult<Account> found = identity == null
                ? accountRepository.search(keyword, safePage, safeSize)
                : accountRepository.search(keyword, identity, safePage, safeSize);
        if (found == null) {
            return AppResult.ok(new UserListResult(List.of(), 0, safePage, safeSize));
        }
        List<UserView> users = found.records().stream()
                .filter(account -> account.status() == AccountStatus.ENABLED)
                .map(account -> new UserView(
                        account.accountId(),
                        account.name(),
                        identityOf(account, identity)))
                .toList();
        return AppResult.ok(new UserListResult(users, users.size(), found.page(), found.size()));
    }

    @Transactional(readOnly = true)
    public AppResult<GuideListResult> listGuides(String credential) {
        Auth auth = authenticate(credential);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        List<GuideVersion> available = guideRepository.findAvailable();
        List<GuideVersionView> versions = available == null
                ? List.of()
                : available.stream()
                        .map(version -> new GuideVersionView(
                                version.version(),
                                version.importedAt(),
                                version.operatorId(),
                                version.scope()))
                        .toList();
        return AppResult.ok(new GuideListResult(versions));
    }

    private Auth authenticate(String credential) {
        LoginUser user = sessionRepository.findByCredential(credential).orElse(null);
        if (user == null) {
            return new Auth(null, null, "登录已失效，请重新登录");
        }
        Account account = accountRepository.findById(user.accountId()).orElse(null);
        if (account == null) {
            return new Auth(user, null, "账号不存在");
        }
        if (!identityRule.isEnabled(account)) {
            return new Auth(user, account, "账号已停用，请联系管理员");
        }
        return new Auth(user, account, null);
    }

    private static IdentityType identityOf(Account account, IdentityType requested) {
        if (requested != null) {
            return requested;
        }
        if (account.roles() == null) {
            return null;
        }
        return account.roles().stream()
                .map(Role::code)
                .filter(Objects::nonNull)
                .map(PublicQueryAppService::parseIdentity)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static IdentityType parseIdentity(String code) {
        try {
            return IdentityType.valueOf(code);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record Auth(LoginUser user, Account account, String failure) {
    }
}
