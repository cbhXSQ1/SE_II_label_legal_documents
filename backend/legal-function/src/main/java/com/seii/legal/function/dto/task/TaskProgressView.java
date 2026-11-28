package com.seii.legal.function.dto.task;

import java.util.List;

public record TaskProgressView(
        int due,
        int submitted,
        int unsubmitted,
        int conflictCount,
        int confirmedCount,
        List<AnnotatorProgress> annotators
) {
}
