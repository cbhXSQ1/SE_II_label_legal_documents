package com.seii.legal.data.entity;

public record DifferenceItem(
        String answerId,
        String questionId,
        String dimension,
        String object,
        String standardValue,
        String studentValue,
        String note
) {
}
