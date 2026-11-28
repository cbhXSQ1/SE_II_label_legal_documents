package com.seii.legal.function.dto.publicquery;

import java.util.List;

public record UserListResult(List<UserView> users, long total, int page, int size) {
    public List<UserView> records() {
        return users;
    }
}
