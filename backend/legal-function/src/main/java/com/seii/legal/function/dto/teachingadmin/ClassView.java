package com.seii.legal.function.dto.teachingadmin;

public record ClassView(
        String id,
        String code,
        String name,
        String term,
        String teacher,
        long studentCount
) {
}
