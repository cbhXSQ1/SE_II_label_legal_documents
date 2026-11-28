package com.seii.legal.data.entity;

public record TeachingClass(
        String classId,
        String classCode,
        String name,
        String term,
        String teacherId,
        int studentCount
) {
}
