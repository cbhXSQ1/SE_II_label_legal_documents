package com.seii.legal.function.dto.publicquery;

import java.time.LocalDateTime;

public record GuideVersionView(
        String version,
        LocalDateTime importedAt,
        String operator,
        String scope
) {
}
