package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record Proposition(
        String propositionId,
        String elementId,
        String content,
        String type,
        LocalDateTime createdAt
) {
}
