package com.seii.legal.data.entity;

public record Agreement(double agreement, double difference, java.util.List<DiffItem> items) {
}
