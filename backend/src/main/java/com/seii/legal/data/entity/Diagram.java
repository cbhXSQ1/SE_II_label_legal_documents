package com.seii.legal.data.entity;

public record Diagram(
        String diagramId,
        String relationVersionId,
        String title,
        String nodesJson,
        String relNodesJson,
        String edgesJson,
        String layoutJson,
        int version,
        String reviewStatus
) {
}
