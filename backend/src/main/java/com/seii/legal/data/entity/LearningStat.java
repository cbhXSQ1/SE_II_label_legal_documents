package com.seii.legal.data.entity;

public record LearningStat(
        String studentId,
        String classId,
        String pointId,
        String exerciseId,
        int practiceCount,
        double bestScore,
        double recentScore,
        String completion
) {
}
