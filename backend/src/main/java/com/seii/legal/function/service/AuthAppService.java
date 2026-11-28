package com.seii.legal.function.service;

import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.AuthDtos;
import com.seii.legal.rule.identity.IdentityRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证与身份功能层服务（AUTH-01…05）。
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private static final long SESSION_EXPIRE_HOURS = 12L;

    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationRepository applicationRepository;
    private final IdentityRule identityRule;
    private final AuthActionTemplate authTemplate;

    @Transactional
    public AppResult<AuthDtos.SessionResponse> login(AuthDtos.LoginRequest request) {
        Account account = accountRepository.findByAccount(request.account()).orElse(null);
        if (account == null || account.password() == null || !account.password().equals(request.password())) {
            return AppResult.fail("账号或密码错误");
        }
        if (!identityRule.isEnabled(account)) {
            return AppResult.fail("账号已停用，请联系管理员");
        }
        List<IdentityRule.IdentityOption> rawOptions = identityRule.availableIdentities(account);
        if (rawOptions == null || rawOptions.isEmpty()) {
            return AppResult.fail("没有可进入的系统，请联系管理员");
        }

        List<AuthDtos.IdentityOptionDto> options = rawOptions.stream().map(this::toOptionDto).toList();
        boolean single = options.size() == 1;
        AuthDtos.IdentityOptionDto sole = single ? options.get(0) : null;
        SystemType system = single ? sole.system() : null;
        IdentityType identity = single ? sole.identity() : null;

        LocalDateTime expiresAt = TimeFormats.now().plusHours(SESSION_EXPIRE_HOURS);
        String credential = sessionRepository.save(new LoginUser(account.accountId(), system, identity, null, expiresAt));

        AuthDtos.SessionResponse response = new AuthDtos.SessionResponse(
                credential,
                account.accountId(),
                account.name(),
                account.unit(),
                system,
                identity,
                system == null ? "" : system.displayName(),
                identity == null ? "" : identity.displayName(),
                single,
                sole,
                options
        );
        List<String> actions = single ? List.of("进入首页") : List.of("选择系统与身份");
        return AppResult.ok(response, actions);
    }

    @Transactional
    public AppResult<AuthDtos.SessionResponse> selectIdentity(String credential, AuthDtos.SelectIdentityRequest request) {
        return authTemplate.executeAuthenticated(credential, ctx -> {
            Account account = ctx.account();
            if (!identityRule.hasRole(account, request.identity())) {
                return AppResult.fail("当前账号没有该身份");
            }
            List<AuthDtos.IdentityOptionDto> options = identityRule.availableIdentities(account).stream()
                    .map(this::toOptionDto)
                    .toList();
            AuthDtos.IdentityOptionDto matched = options.stream()
                    .filter(opt -> opt.system() == request.system() && opt.identity() == request.identity())
                    .findFirst()
                    .orElse(null);
            if (matched == null) {
                return AppResult.fail("所选系统与身份不匹配");
            }

            sessionRepository.delete(credential);
            LocalDateTime expiresAt = TimeFormats.now().plusHours(SESSION_EXPIRE_HOURS);
            String newCredential = sessionRepository.save(
                    new LoginUser(account.accountId(), request.system(), request.identity(), null, expiresAt));

            AuthDtos.SessionResponse response = new AuthDtos.SessionResponse(
                    newCredential,
                    account.accountId(),
                    account.name(),
                    account.unit(),
                    request.system(),
                    request.identity(),
                    request.system().displayName(),
                    request.identity().displayName(),
                    options.size() == 1,
                    matched,
                    options
            );
            return AppResult.ok(response, List.of("进入首页"));
        });
    }

    @Transactional
    public AppResult<Void> logout(String credential) {
        if (credential != null && !credential.isBlank()) {
            sessionRepository.delete(credential);
        }
        return AppResult.ok(null);
    }

    @Transactional
    public AppResult<AuthDtos.ApplyAccountResponse> apply(AuthDtos.ApplyAccountRequest request) {
        if (request.system() == SystemType.TEACHING && request.identity() == IdentityType.STUDENT
                && (request.studentNo() == null || request.studentNo().isBlank())) {
            return AppResult.fail("申请教学系统学生账号时请填写学号");
        }
        if (request.system() == SystemType.TEACHING && request.identity() == IdentityType.TEACHER
                && (request.teacherNo() == null || request.teacherNo().isBlank())) {
            return AppResult.fail("申请教学系统教师账号时请填写教师工号");
        }
        AccountApplication application = new AccountApplication(
                null,
                request.name().trim(),
                request.unit().trim(),
                request.system(),
                request.identity(),
                request.studentNo(),
                request.teacherNo(),
                request.note(),
                TimeFormats.now(),
                ApplicationStatus.PENDING
        );
        String applicationId = applicationRepository.save(application);
        return AppResult.ok(new AuthDtos.ApplyAccountResponse(applicationId), List.of("等待管理员审批"));
    }

    @Transactional(readOnly = true)
    public AppResult<AuthDtos.AuthOptionsResponse> options(String credential) {
        return authTemplate.executeAuthenticated(credential, ctx -> {
            Account account = ctx.account();
            List<AuthDtos.IdentityOptionDto> options = identityRule.availableIdentities(account).stream()
                    .map(this::toOptionDto)
                    .toList();
            AuthDtos.AuthOptionsResponse response = new AuthDtos.AuthOptionsResponse(
                    account.name(),
                    options,
                    ctx.user().system(),
                    ctx.user().identity()
            );
            return AppResult.ok(response);
        });
    }

    private AuthDtos.IdentityOptionDto toOptionDto(IdentityRule.IdentityOption opt) {
        return new AuthDtos.IdentityOptionDto(
                opt.system(),
                opt.identity(),
                opt.system().displayName(),
                opt.identity().displayName(),
                opt.description()
        );
    }
}
