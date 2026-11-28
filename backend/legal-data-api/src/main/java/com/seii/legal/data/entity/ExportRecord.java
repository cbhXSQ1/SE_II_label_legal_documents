package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record ExportRecord(
        String exportId,
        String exporterId,
        LocalDateTime exportedAt,
        String taskId,
        String guideVersion,
        String fileName,
        String format,
        String requestId
) {
}
