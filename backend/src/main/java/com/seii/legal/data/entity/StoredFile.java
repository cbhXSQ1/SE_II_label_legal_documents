package com.seii.legal.data.entity;

import java.time.LocalDateTime;

public record StoredFile(
        String fileId,
        String fileName,
        String format,
        byte[] content,
        String downloadUrl,
        String uploaderId,
        LocalDateTime uploadedAt,
        String type
) {
}
