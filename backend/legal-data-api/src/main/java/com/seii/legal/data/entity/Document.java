package com.seii.legal.data.entity;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import java.time.LocalDateTime;

public record Document(
        String documentId,
        String title,
        String caseNo,
        String documentType,
        String content,
        String fileId,
        String uploaderId,
        LocalDateTime uploadedAt,
        String scope,
        SystemType sourceSystem
) {
}
