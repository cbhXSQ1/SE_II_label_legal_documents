package com.seii.legal.function.dto.task;

import com.seii.legal.common.enums.TaskStatus;
import java.util.List;

public record TaskListItem(
        String id,
        String name,
        String guide,
        String due,
        TaskStatus status,
        boolean teaching,
        List<String> annotators,
        String arbiter,
        String documentId
) {
}
