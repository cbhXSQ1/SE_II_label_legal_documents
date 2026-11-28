package com.seii.legal.function.dto.teacher;

import com.seii.legal.data.entity.Question;
import java.util.List;

public record TargetedView(List<TargetedPointView> points, List<Question> questions) {
}
