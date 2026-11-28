package com.seii.legal.function.support;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaskDocumentAdapter {

    public List<String> toDocumentIds(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return List.of();
        }
        return List.of(documentId);
    }

    public String toDocumentId(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return null;
        }
        return documentIds.get(0);
    }
}
