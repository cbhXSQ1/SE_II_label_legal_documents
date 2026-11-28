package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;

public record Role(String code, String name, SystemType system) {
}
