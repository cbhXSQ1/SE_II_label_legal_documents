package com.seii.legal.function.dto.adjudication;

public record DifferenceItemView(
        String id,
        String type,
        String object,
        String valueA,
        String valueB,
        String note
) {
}
