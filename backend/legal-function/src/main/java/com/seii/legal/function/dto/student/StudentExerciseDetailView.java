package com.seii.legal.function.dto.student;

import com.seii.legal.data.entity.Question;
import java.time.LocalDateTime;
import java.util.List;

public record StudentExerciseDetailView(
        String id,
        String name,
        LocalDateTime deadline,
        boolean allowRedo,
        String status,
        List<Question> questions
) {
}
