package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record ImportRow(
        int rowNo,
        String classNo,
        String studentNo,
        String name,
        String teacherNo,
        List<String> errors
) {
}
