package com.seii.legal.data.entity;

import java.util.List;

public record Agreement(
        double agreement,
        double difference,
        List<DiffItem> items
) {
}
