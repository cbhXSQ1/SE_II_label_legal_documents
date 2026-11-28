package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record AnswerRecord(
        String answerId,
        String exerciseId,
        String studentId,
        String questionId,
        String answer,
        LocalDateTime submittedAt,
        double score,
        String status,
        String feedback,
        String teacherId
) {
}
