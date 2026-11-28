package com.seii.legal.data.entity;

import java.util.List;

public record Relation(
        String relationId,
        String type,
        List<String> memberIds,
        String direction,
        String formalExpression,
        int nestingLevel
) {
}
