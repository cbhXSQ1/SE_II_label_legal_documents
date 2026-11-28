package com.seii.legal.function.dto.student;

import java.util.List;

public record AnswerSubmitCommand(String exerciseId, List<AnswerDraftItem> answers, String requestId) {
}
