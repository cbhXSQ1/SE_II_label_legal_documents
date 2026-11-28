package com.seii.legal.function.dto.student;

import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.LearningStat;
import java.util.List;

public record LearningView(
        List<MaterialView> materials,
        List<AnswerRecord> answers,
        List<LearningStat> stats
) {
}
