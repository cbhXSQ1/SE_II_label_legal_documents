package com.seii.legal.function.dto.annotation;

import com.seii.legal.function.support.AnnotationPayloadAdapter;
import java.util.List;
import java.util.Map;

public record SaveDraftCommand(
        String taskId,
        String documentId,
        AnnotationPayloadAdapter.AnnotationContent content,
        List<Map<String, Object>> pieces,
        List<Map<String, Object>> segments,
        List<Map<String, Object>> relations,
        List<Map<String, Object>> diagram
) {
}
