package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import java.time.LocalDateTime;
import java.util.List;

public record Element(
        String elementId,
        String cutVersionId,
        String versionId,
        String paragraph,
        String typeCode,
        String subtype,
        int sequence,
        int startOffset,
        int endOffset,
        boolean proposition,
        String difficultQuestion,
        String content
) {
}
