package com.seii.legal.data.entity;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import java.time.LocalDateTime;

public record Submission(
        String submissionId,
        String taskId,
        String documentId,
        RoundType round,
        String roundId,
        String annotatorId,
        LocalDateTime submittedAt,
        String guideVersion,
        String content,
        SubmissionStatus status,
        String returnReason
) {
}
