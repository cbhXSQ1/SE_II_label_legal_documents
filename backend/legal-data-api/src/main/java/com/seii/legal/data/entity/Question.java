package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
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
