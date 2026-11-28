package com.seii.legal.data.entity;

import java.time.LocalDateTime;
import java.util.List;

public record LearningStat(
        int practiceCount,
        double bestScore,
        double latestScore,
        String completionStatus
) {
}
