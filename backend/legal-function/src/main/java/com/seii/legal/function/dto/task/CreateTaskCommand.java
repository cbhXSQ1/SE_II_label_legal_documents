package com.seii.legal.function.dto.task;

import java.util.List;

public record CreateTaskCommand(
        String name,
        String description,
        String deadline,
        String guideVersion,
        String documentId,
        List<String> annotatorIds,
        String arbiterId,
        boolean draft
) {
}
