package com.seii.legal.function.dto.student;

import java.util.List;

public record AnswerDraftCommand(String exerciseId, List<AnswerDraftItem> answers) {
}
