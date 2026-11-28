package com.seii.legal.function.support;

import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.rule.identity.IdentityRule;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class AuthActionTemplate {

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final IdentityRule identityRule;

    public AuthActionTemplate(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            IdentityRule identityRule) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.identityRule = identityRule;
    }

    public record AuthContext(LoginUser user, Account account, DataScope scope) {
    }

    public <T> AppResult<T> execute(
            String credential,
            Set<IdentityType> allowedIdentities,
            String taskId,
            String classId,
            String forbiddenMessage,
            Function<AuthContext, AppResult<T>> action) {
        LoginUser user = sessionRepository.findByCredential(credential)
                .orElseThrow(() -> new IllegalStateException("登录已失效，请重新登录"));
        Account account = accountRepository.findById(user.accountId())
                .orElseThrow(() -> new IllegalStateException("账号不存在"));
        if (!identityRule.isEnabled(account)) {
            return AppResult.fail("账号已停用，请联系管理员");
        }
        if (allowedIdentities != null && !allowedIdentities.isEmpty()) {
            boolean permitted = allowedIdentities.contains(user.identity())
                    && identityRule.hasRole(account, user.identity());
            if (!permitted) {
                return AppResult.fail(forbiddenMessage);
            }
        }
        DataScope scope = identityRule.resolveScope(user, taskId, classId);
        return action.apply(new AuthContext(user, account, scope));
    }
}
