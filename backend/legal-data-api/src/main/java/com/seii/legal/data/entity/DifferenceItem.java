package com.seii.legal.data.entity;

import com.seii.legal.common.enums.QuestionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DifferenceItem(
        String type,
        String object,
        String valueA,
        String valueB,
        String note
) {
}
