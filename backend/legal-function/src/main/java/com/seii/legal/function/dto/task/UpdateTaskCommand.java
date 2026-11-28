package com.seii.legal.function.dto.task;

import java.util.List;

public record UpdateTaskCommand(
        String description,
        String deadline,
        List<String> annotatorIds,
        String arbiterId
) {
}
