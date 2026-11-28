package com.seii.legal.data.entity;

public record Proposition(
        String propositionId,
        String elementId,
        String text,
        String basicType,
        String subType
) {
}
