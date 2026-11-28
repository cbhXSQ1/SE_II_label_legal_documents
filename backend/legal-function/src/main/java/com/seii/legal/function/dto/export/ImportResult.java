package com.seii.legal.function.dto.export;

import com.seii.legal.rule.export.ExportRule;

public record ImportResult(
        String taskId,
        ExportRule.ImportPreview preview
) {
}
