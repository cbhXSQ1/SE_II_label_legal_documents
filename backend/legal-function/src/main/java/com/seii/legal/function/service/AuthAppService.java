package com.seii.legal.function.service;

import com.seii.legal.common.enums.ApplicationStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.ApplyCommand;
import com.seii.legal.function.dto.ApplyResult;
import com.seii.legal.function.dto.IdentityOptionView;
import com.seii.legal.function.dto.LoginCommand;
import com.seii.legal.function.dto.OptionsView;
import com.seii.legal.function.dto.SelectCommand;
import com.seii.legal.function.dto.SessionView;
import com.seii.legal.rule.identity.IdentityRule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAppService {

    private static final int SESSION_HOURS = 8;

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final ApplicationRepository applicationRepository;
    private final IdentityRule identityRule;

    public AuthAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            ApplicationRepository applicationRepository,
            IdentityRule identityRule) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.applicationRepository = applicationRepository;
        this.identityRule = identityRule;
    }

    @Transactional
    public AppResult<SessionView> login(LoginCommand command) {
        if (command.account() == null || command.account().isBlank()
                || command.password() == null || command.password().isBlank()) {
            return AppResult.fail("账号或密码错误");
        }
        Account account = accountRepository.findByAccount(command.account()).orElse(null);
        if (account == null || !Objects.equals(account.password(), command.password())) {
            return AppResult.fail("账号或密码错误");
        }
        if (!identityRule.isEnabled(account)) {
            return AppResult.fail("账号已停用，请联系管理员");
        }
        List<IdentityRule.IdentityOption> options = availableOptions(account);
        if (options.isEmpty()) {
            return AppResult.fail("没有可进入的系统，请联系管理员");
        }
        boolean single = options.size() == 1;
        IdentityRule.IdentityOption only = single ? options.get(0) : null;
        LoginUser session = new LoginUser(
                account.accountId(),
                single ? only.system() : null,
                single ? only.identity() : null,
                null,
                LocalDateTime.now().plusHours(SESSION_HOURS));
        String credential = sessionRepository.save(session);
        List<IdentityOptionView> views = options.stream().map(AuthAppService::toView).toList();
        return AppResult.ok(new SessionView(
                credential,
                account.accountId(),
                account.name(),
                account.unit(),
                single ? only.system() : null,
                single ? only.identity() : null,
                single ? systemName(only.system()) : null,
                single ? identityName(only.identity()) : null,
                single,
                views));
    }

    @Transactional
    public AppResult<SessionView> select(String credential, SelectCommand command) {
        LoginUser user = sessionRepository.findByCredential(credential)
                .orElseThrow(() -> new IllegalStateException("登录已失效，请重新登录"));
        Account account = accountRepository.findById(user.accountId())
                .orElseThrow(() -> new IllegalStateException("账号不存在"));
        List<IdentityRule.IdentityOption> options = availableOptions(account);
        IdentityRule.IdentityOption selected = options.stream()
                .filter(option -> option.system() == command.system() && option.identity() == command.identity())
                .findFirst()
                .orElse(null);
        if (selected == null || !identityRule.hasRole(account, command.identity())) {
            return AppResult.fail("账号没有该身份");
        }
        LoginUser updated = new LoginUser(
                user.accountId(),
                command.system(),
                command.identity(),
                user.credential(),
                user.expiresAt());
        String savedCredential = sessionRepository.save(updated);
        List<IdentityOptionView> views = options.stream().map(AuthAppService::toView).toList();
        return AppResult.ok(new SessionView(
                savedCredential,
                account.accountId(),
                account.name(),
                account.unit(),
                command.system(),
                command.identity(),
                systemName(command.system()),
                identityName(command.identity()),
                options.size() == 1,
                views));
    }

    @Transactional
    public AppResult<Void> logout(String credential) {
        sessionRepository.delete(credential);
        return AppResult.ok(null);
    }

    @Transactional
    public AppResult<ApplyResult> apply(ApplyCommand command) {
        String account = command.studentNo() != null && !command.studentNo().isBlank()
                ? command.studentNo()
                : command.teacherNo();
        AccountApplication application = new AccountApplication(
                null,
                account,
                null,
                command.name(),
                command.unit(),
                command.system(),
                command.identity(),
                ApplicationStatus.PENDING,
                LocalDateTime.now(),
                null,
                null,
                command.note());
        String applicationId = applicationRepository.save(application);
        return AppResult.ok(new ApplyResult(applicationId));
    }

    @Transactional(readOnly = true)
    public AppResult<OptionsView> options(String credential) {
        LoginUser user = sessionRepository.findByCredential(credential)
                .orElseThrow(() -> new IllegalStateException("登录已失效，请重新登录"));
        Account account = accountRepository.findById(user.accountId())
                .orElseThrow(() -> new IllegalStateException("账号不存在"));
        List<IdentityOptionView> views = availableOptions(account).stream()
                .map(AuthAppService::toView)
                .toList();
        return AppResult.ok(new OptionsView(account.name(), views, user.system(), user.identity()));
    }

    private List<IdentityRule.IdentityOption> availableOptions(Account account) {
        List<IdentityRule.IdentityOption> options = identityRule.availableIdentities(account);
        return options == null ? List.of() : options;
    }

    private static IdentityOptionView toView(IdentityRule.IdentityOption option) {
        return new IdentityOptionView(
                option.system(),
                option.identity(),
                systemName(option.system()),
                identityName(option.identity()),
                option.description());
    }

    private static String systemName(SystemType system) {
        if (system == null) {
            return null;
        }
        return switch (system) {
            case ADMIN -> "管理后台";
            case ANNOTATION -> "普通标注系统";
            case TEACHING -> "教学系统";
        };
    }

    private static String identityName(IdentityType identity) {
        if (identity == null) {
            return null;
        }
        return switch (identity) {
            case ADMIN -> "管理员";
            case TEACHING_ADMIN -> "教学管理员";
            case TASK_MANAGER -> "任务管理者";
            case TEACHER -> "教师";
            case ANNOTATOR -> "标注员";
            case STUDENT -> "学生";
            case ARBITER -> "仲裁员";
            case EXPERT -> "领域专家";
        };
    }
}
