package com.seii.legal.data.entity;

import com.seii.legal.common.enums.QuestionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DiagramReview(
        String reviewId,
        String taskId,
        String documentId,
        String diagramId,
        String reviewerId,
        LocalDateTime reviewedAt,
        String result,
        List<String> errors
) {
}
