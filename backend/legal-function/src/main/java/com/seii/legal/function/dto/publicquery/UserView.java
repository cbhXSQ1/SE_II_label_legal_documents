package com.seii.legal.function.dto.publicquery;

import com.seii.legal.common.enums.IdentityType;

public record UserView(String id, String name, IdentityType identity) {
}
