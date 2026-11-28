package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import java.time.LocalDateTime;
import java.util.List;

public record Relation(
        String relationId,
        String elementVersionId,
        String relationNo,
        String type,
        List<String> elementIds,
        String direction,
        String formalExpression,
        int nestingLevel
) {
}
