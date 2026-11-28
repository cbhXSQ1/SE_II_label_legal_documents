package com.seii.legal.function.dto.teacher;

import java.util.List;

public record StudentLearningView(
        String id,
        String name,
        int practiceCount,
        double bestScore,
        double recentScore,
        String completion,
        List<String> weakPoints
) {
}
