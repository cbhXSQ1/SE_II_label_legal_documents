package com.seii.legal.data.repository;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(String accountId);

    Optional<Account> findByAccount(String account);

    PageResult<Account> search(String keyword, int page, int size);

    PageResult<Account> search(String keyword, IdentityType identity, int page, int size);

    String save(Account account);

    void updateStatus(String accountId, AccountStatus status, String operatorId, LocalDateTime time);

    List<Role> findRoles(String accountId);

    void saveRoles(String accountId, List<Role> roles, String operatorId, LocalDateTime time);

    void saveChangeLog(AccountChangeLog log);
}
