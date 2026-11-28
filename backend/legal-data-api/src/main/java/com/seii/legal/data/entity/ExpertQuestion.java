package com.seii.legal.data.entity;

import com.seii.legal.common.enums.QuestionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ExpertQuestion(
        String questionId,
        String taskId,
        String documentId,
        String conflictId,
        String expertId,
        String content,
        QuestionStatus status,
        LocalDateTime askedAt,
        LocalDateTime repliedAt
) {
}
