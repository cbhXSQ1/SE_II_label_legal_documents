package com.seii.legal.data.entity;

import com.seii.legal.common.enums.QuestionStatus;

import java.time.LocalDateTime;

public record ExpertQuestion(
        String questionId,
        String taskId,
        String documentId,
        String conflictId,
        String guideClause,
        String conflictLabel,
        String annotatorResults,
        String submitterId,
        String expertId,
        QuestionStatus status,
        String note,
        LocalDateTime submittedAt
) {
}
