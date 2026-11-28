package com.seii.legal.function.dto.student;

import com.seii.legal.data.entity.AnswerRecord;
import java.util.List;

public record StudentAnswersView(List<AnswerRecord> answers) {
}
