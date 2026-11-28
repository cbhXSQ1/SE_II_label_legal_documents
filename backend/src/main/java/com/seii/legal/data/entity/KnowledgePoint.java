package com.seii.legal.data.entity;

public record KnowledgePoint(
        String pointId,
        String teacherId,
        String name,
        String category,
        String description,
        String difficulty
) {
}
