package com.seii.legal.function.dto.student;

import com.seii.legal.data.entity.DifferenceItem;
import java.util.List;

public record SubmitAnswerResult(
        String answerId,
        double score,
        String status,
        List<DifferenceItem> differences,
        String systemFeedback
) {
}
