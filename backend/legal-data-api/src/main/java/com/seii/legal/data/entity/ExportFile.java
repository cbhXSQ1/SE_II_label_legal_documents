package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record ExportFile(
        String fileId,
        String exportId,
        String fileName,
        String format,
        byte[] content,
        LocalDateTime savedAt
) {
}
