package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherFeedback(
        String answerId,
        String teacherId,
        String content,
        LocalDateTime createdAt
) {
}
