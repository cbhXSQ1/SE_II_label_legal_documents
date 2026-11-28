package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record TeacherFeedback(
        String feedbackId,
        String answerId,
        String teacherId,
        String content,
        LocalDateTime feedbackAt
) {
}
