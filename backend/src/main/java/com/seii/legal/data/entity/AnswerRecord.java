package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record AnswerRecord(
        String answerId,
        String studentId,
        String exerciseId,
        String questionVersionJson,
        String contentJson,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        long elapsedSeconds,
        Double score,
        String status,
        String systemFeedback
) {
}
