package com.seii.legal.function.dto.teachingadmin;

import java.util.List;

public record ImportResult(
        String importId,
        int classCount,
        int studentCount,
        List<String> duplicateStudentNos,
        List<ImportFailure> missingRows,
        List<String> unmatchedTeachers
) {
}
