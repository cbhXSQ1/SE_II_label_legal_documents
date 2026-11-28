package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import java.time.LocalDateTime;

public record TaskRound(
        String roundId,
        String taskId,
        String documentId,
        RoundType type,
        int sequence,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {}
