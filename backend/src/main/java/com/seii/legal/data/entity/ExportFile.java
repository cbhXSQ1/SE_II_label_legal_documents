package com.seii.legal.data.entity;

public record ExportFile(
        String fileId,
        String exportId,
        String fileName,
        String format,
        byte[] content
) {
}
