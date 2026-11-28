package com.seii.legal.function.dto;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;

public record SelectCommand(SystemType system, IdentityType identity) {
}
