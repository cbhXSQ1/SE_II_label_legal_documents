package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;

import java.time.LocalDateTime;

public record Document(
        String documentId,
        String title,
        String caseNo,
        String type,
        String rawText,
        String reasonText,
        String annotationRange,
        String fileId,
        SystemType system,
        String uploaderId,
        LocalDateTime uploadedAt
) {
}
