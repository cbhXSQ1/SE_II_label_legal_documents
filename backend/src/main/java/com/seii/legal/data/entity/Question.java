package com.seii.legal.data.entity;

import java.util.List;

public record Question(
        String questionId,
        String classId,
        String teacherId,
        String type,
        String stem,
        String documentId,
        String difficulty,
        List<String> pointIds,
        String standardJson,
        List<String> options,
        String answer,
        String guideVersion,
        int version,
        String status
) {
}
