package com.seii.legal.function.dto.adjudication;

public record FinalContentView(
        String roundId,
        String versionType,
        String cutVersionId,
        String elementVersionId,
        String relationVersionId,
        String diagramId
) {
}
