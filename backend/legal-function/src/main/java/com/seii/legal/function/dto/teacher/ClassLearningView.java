package com.seii.legal.function.dto.teacher;

import java.util.List;

public record ClassLearningView(List<StudentLearningView> students, int threshold) {
}
