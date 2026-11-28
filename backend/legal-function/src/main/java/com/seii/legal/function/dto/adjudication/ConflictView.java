package com.seii.legal.function.dto.adjudication;

public record ConflictView(
        String conflictId,
        String differenceType,
        String differenceObject,
        String valueA,
        String valueB,
        String method,
        String result,
        boolean decided
) {
}
