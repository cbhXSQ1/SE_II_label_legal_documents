package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import java.time.LocalDateTime;
import java.util.List;

public record CutVersion(
        String versionId,
        String taskId,
        String documentId,
        String annotatorId,
        RoundType round,
        String roundId,
        List<String> cutPoints,
        List<String> paragraphIds,
        LocalDateTime savedAt
) {
}
