package com.seii.legal.function.dto.teachingadmin;

import java.util.List;

public record ImportResultView(
        int successClasses,
        int successStudents,
        List<ImportFailure> failedRows,
        String downloadUrl
) {
}
