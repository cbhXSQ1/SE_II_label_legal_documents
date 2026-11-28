package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.function.dto.PublicQueryDtos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 公共只读查询服务（USER-01、GUIDE-01）。
 */
@Service
@RequiredArgsConstructor
public class PublicQueryAppService {

    private final AuthActionTemplate authTemplate;
    private final AccountRepository accountRepository;
    private final GuideRepository guideRepository;

    @Transactional(readOnly = true)
    public AppResult<PublicQueryDtos.UserListResponse> listUsers(
            String credential, IdentityType identity, String keyword) {
        return authTemplate.executeAuthenticated(credential, ctx -> {
            PageResult<Account> page = (identity != null)
                    ? accountRepository.search(keyword, identity, 1, 200)
                    : accountRepository.search(keyword, 1, 200);
            List<PublicQueryDtos.UserOption> options = new ArrayList<>();
            for (Account account : page.records()) {
                if (account.status() == AccountStatus.DISABLED) {
                    continue;
                }
                List<Role> roles = account.roles() == null ? List.of() : account.roles();
                if (identity != null) {
                    options.add(new PublicQueryDtos.UserOption(account.accountId(), account.name(), identity.name()));
                } else {
                    for (Role role : roles) {
                        options.add(new PublicQueryDtos.UserOption(account.accountId(), account.name(), role.code()));
                    }
                }
            }
            return AppResult.ok(new PublicQueryDtos.UserListResponse(options));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PublicQueryDtos.GuideOptionListResponse> listGuides(String credential) {
        return authTemplate.executeAuthenticated(credential, ctx -> {
            List<GuideVersion> versions = guideRepository.findAvailable();
            List<PublicQueryDtos.GuideOption> items = versions.stream()
                    .map(g -> new PublicQueryDtos.GuideOption(
                            g.version(),
                            TimeFormats.format(g.importedAt()),
                            resolveName(g.operatorId()),
                            g.scope()
                    ))
                    .toList();
            return AppResult.ok(new PublicQueryDtos.GuideOptionListResponse(items));
        });
    }

    private String resolveName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }
}
