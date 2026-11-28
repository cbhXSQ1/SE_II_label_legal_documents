package com.seii.legal.function.dto.adjudication;

public record FinalConfirmCommand(
        String taskId,
        String documentId
) {
}
