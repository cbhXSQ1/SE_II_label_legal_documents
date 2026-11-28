package com.seii.legal.function.dto.annotation;

import com.seii.legal.function.support.AnnotationPayloadAdapter;

public record LoadAnnotationView(
        AnnotationPayloadAdapter.AnnotationContent content,
        boolean submitted,
        String round
) {
}
