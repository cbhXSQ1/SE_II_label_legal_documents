package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record ClassMember(
        String classId,
        String studentId,
        String studentNo,
        LocalDateTime joinedAt,
        String status
) {
}
