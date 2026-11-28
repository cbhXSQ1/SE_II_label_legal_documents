package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionVersion(
        String questionId,
        int version,
        String standardAnswer,
        String guideVersion,
        LocalDateTime effectiveAt
) {
}
