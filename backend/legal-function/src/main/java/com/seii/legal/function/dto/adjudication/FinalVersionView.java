package com.seii.legal.function.dto.adjudication;

import java.time.LocalDateTime;

public record FinalVersionView(
        FinalContentView content,
        String guideVersion,
        String confirmedBy,
        LocalDateTime confirmedAt,
        String status
) {
}
