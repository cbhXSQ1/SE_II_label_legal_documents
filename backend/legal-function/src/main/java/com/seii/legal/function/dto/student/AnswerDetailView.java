package com.seii.legal.function.dto.student;

import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.TeacherFeedback;
import java.util.List;

public record AnswerDetailView(
        AnswerRecord answer,
        List<DifferenceItem> differences,
        String systemFeedback,
        TeacherFeedback teacherFeedback
) {
}
