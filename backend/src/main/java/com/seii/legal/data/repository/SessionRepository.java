package com.seii.legal.data.repository;

import com.seii.legal.common.enums.LoginUser;

import java.util.Optional;

public interface SessionRepository {
    String save(LoginUser user);

    Optional<LoginUser> findByCredential(String credential);

    void delete(String credential);
}
