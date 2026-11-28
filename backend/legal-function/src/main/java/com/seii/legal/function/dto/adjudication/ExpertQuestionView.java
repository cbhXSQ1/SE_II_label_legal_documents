package com.seii.legal.function.dto.adjudication;

import com.seii.legal.common.enums.QuestionStatus;
import java.time.LocalDateTime;

public record ExpertQuestionView(
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
