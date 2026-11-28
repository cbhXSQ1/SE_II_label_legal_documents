package com.seii.legal.function.dto.teacher;

import java.util.List;

public record QuestionCommand(
        String id,
        String type,
        String stem,
        String documentId,
        String difficulty,
        List<String> pointIds,
        List<String> options,
        String answer,
        Object standard,
        String guideVersion,
        String classId
) {
}
