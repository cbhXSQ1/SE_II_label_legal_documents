package com.seii.legal.function.service;

import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.result.AuthException;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.rule.identity.IdentityRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Function;

/**
 * 模板方法模式：统一执行 凭证核验 -> 账号启用检查 -> 本次身份与角色校验 -> 数据范围解析 -> 业务回调。
 */
@Component
@RequiredArgsConstructor
public class AuthActionTemplate {

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final IdentityRule identityRule;

    public record AuthContext(LoginUser user, Account account, DataScope scope) {
    }

    /**
     * 仅要求已登录且账号处于启用状态（不限制具体身份）。
     */
    public <T> AppResult<T> executeAuthenticated(
            String credential,
            Function<AuthContext, AppResult<T>> action) {
        return execute(credential, null, null, null, null, action);
    }

    /**
     * 完整鉴权模板：核验登录凭证、账号启用状态、本次身份角色权限与数据范围。
     */
    public <T> AppResult<T> execute(
            String credential,
            Set<IdentityType> allowedIdentities,
            String taskId,
            String classId,
            String forbiddenMessage,
            Function<AuthContext, AppResult<T>> action) {
        if (credential == null || credential.isBlank()) {
            throw new AuthException("登录已失效，请重新登录");
        }
        LoginUser user = sessionRepository.findByCredential(credential)
                .orElseThrow(() -> new AuthException("登录已失效，请重新登录"));
        Account account = accountRepository.findById(user.accountId())
                .orElseThrow(() -> new BusinessException("账号不存在"));
        if (!identityRule.isEnabled(account)) {
            throw new AuthException("账号已停用，请联系管理员");
        }
        if (allowedIdentities != null && !allowedIdentities.isEmpty()) {
            boolean permitted = user.identity() != null
                    && allowedIdentities.contains(user.identity())
                    && identityRule.hasRole(account, user.identity());
            if (!permitted) {
                return AppResult.fail(forbiddenMessage == null ? "当前身份无权执行该操作" : forbiddenMessage);
            }
        }
        DataScope scope = user.identity() == null ? null : identityRule.resolveScope(user, taskId, classId);
        return action.apply(new AuthContext(user, account, scope));
    }
}
