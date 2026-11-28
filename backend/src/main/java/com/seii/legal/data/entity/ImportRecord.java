package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record ImportRecord(
        String importId,
        String operatorId,
        LocalDateTime importedAt,
        String fileId,
        int successClasses,
        int successStudents,
        List<ImportRow> failedRows,
        String resultFileId
) {
}
