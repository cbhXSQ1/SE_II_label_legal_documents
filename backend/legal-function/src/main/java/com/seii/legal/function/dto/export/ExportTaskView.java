package com.seii.legal.function.dto.export;

public record ExportTaskView(
        String id,
        String name,
        String confirmedAt,
        int annotatorCount,
        String guideVersion
) {
}
