package com.seii.legal.function.dto.document;

public record DocumentListItemView(
        String id,
        String title,
        String caseNo,
        String type,
        String taskId
) {
}
