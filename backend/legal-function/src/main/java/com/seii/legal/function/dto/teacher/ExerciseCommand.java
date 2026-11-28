package com.seii.legal.function.dto.teacher;

import java.time.LocalDateTime;
import java.util.List;

public record ExerciseCommand(
        String id,
        String name,
        String description,
        LocalDateTime deadline,
        Boolean allowRedo,
        String guideVersion,
        String classId,
        List<String> questionIds
) {
}
