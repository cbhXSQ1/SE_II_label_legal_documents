package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgePoint(
        String pointId,
        String name,
        String description,
        String parentId,
        String category,
        String teacherId,
        LocalDateTime createdAt
) {
}
