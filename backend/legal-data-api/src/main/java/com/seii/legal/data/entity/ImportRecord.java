package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record ImportRecord(
        String importId,
        String operatorId,
        LocalDateTime importedAt,
        int classCount,
        int studentCount,
        List<ImportRow> failures
) {
}
