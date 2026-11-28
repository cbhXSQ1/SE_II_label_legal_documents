package com.seii.legal.data.entity;

public record TaskProgress(
        String taskId,
        int due,
        int submitted,
        int unsubmitted,
        int conflictCount,
        int confirmedCount,
        Double averageScore
) {
}
