package com.seii.legal.function.dto.adjudication;

public record ExpertQuestionCommand(
        String taskId,
        String conflictId,
        String guideClause,
        String conflictLabel,
        String annotatorResults,
        String expertId,
        String note
) {
}
