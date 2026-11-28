package com.seii.legal.data.entity;

public record ImportRow(
        int rowNumber,
        String classCode,
        String className,
        String term,
        String teacherNo,
        String studentNo,
        String studentName,
        boolean valid,
        String failureReason
) {
}
