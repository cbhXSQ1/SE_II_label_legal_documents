package com.seii.legal.function.dto.adjudication;

public record ExpertReplyCommand(
        String questionId,
        String conclusion,
        String reason,
        Boolean needMore,
        Boolean unanswerable
) {
}
