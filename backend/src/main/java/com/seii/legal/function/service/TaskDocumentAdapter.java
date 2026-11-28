package com.seii.legal.function.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 适配器模式：内部消化页面接口单数 documentId 与数据层 AnnotationTask.documentIds 列表的映射。
 */
@Component
public class TaskDocumentAdapter {

    public List<String> toDocumentIds(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return List.of();
        }
        return List.of(documentId);
    }

    public String primaryDocumentId(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return null;
        }
        return documentIds.get(0);
    }
}
