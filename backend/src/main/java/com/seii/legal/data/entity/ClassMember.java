package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record ClassMember(
        String classId,
        String studentId,
        String studentNo,
        LocalDateTime joinedAt,
        LocalDateTime leftAt,
        String status
) {
}
