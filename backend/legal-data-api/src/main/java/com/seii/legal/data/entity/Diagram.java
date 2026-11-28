package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record Diagram(
        String diagramId,
        String relationVersionId,
        List<String> nodes,
        List<String> edges,
        String layout,
        int version,
        String reviewStatus,
        String createdBy,
        LocalDateTime createdAt
) {
}
