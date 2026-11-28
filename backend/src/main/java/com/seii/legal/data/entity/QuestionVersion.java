package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record QuestionVersion(
        String questionId,
        int version,
        String standardAnswer,
        String guideVersion,
        LocalDateTime effectiveAt
) {
}
