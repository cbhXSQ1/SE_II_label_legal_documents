package com.seii.legal.function.dto.teacher;

public record KnowledgePointCommand(
        String id,
        String name,
        String category,
        String description,
        String difficulty
) {
}
