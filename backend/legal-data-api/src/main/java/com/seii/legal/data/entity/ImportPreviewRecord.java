package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import java.time.LocalDateTime;
import java.util.List;

public record ImportPreviewRecord(
        String importId,
        String taskId,
        String sourceFileId,
        String guideVersion,
        String previewJson,
        LocalDateTime createdAt
) {
}
