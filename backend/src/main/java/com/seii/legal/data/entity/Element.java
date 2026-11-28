package com.seii.legal.data.entity;

public record Element(
        String elementId,
        String paragraphNo,
        String text,
        String typeCode,
        String subType,
        int orderNo,
        int startOffset,
        int endOffset,
        boolean proposition,
        String difficultNote
) {
}
