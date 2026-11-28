package com.seii.legal.data.entity;

public record DiffItem(
        String id,
        String type,
        String object,
        String valueA,
        String valueB,
        String note
) {
    public DiffItem(String type, String object, String valueA, String valueB, String note) {
        this(null, type, object, valueA, valueB, note);
    }
}
