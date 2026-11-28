package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
import java.util.List;

public record TaskRound(
        String roundId,
        String taskId,
        String documentId,
        RoundType type,
        int sequence,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
}
