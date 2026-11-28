package com.seii.legal.function.dto.adjudication;

import java.util.List;

public record CompareResult(
        double agreement,
        double difference,
        String versionA,
        String versionB,
        List<DifferenceItemView> items,
        boolean aboveEighty
) {
}
