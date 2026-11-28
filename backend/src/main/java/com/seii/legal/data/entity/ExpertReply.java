package com.seii.legal.data.entity;

import com.seii.legal.common.enums.QuestionStatus;

import java.time.LocalDateTime;

public record ExpertReply(
        String replyId,
        String questionId,
        String conclusion,
        String reason,
        String expertId,
        LocalDateTime repliedAt,
        String referenceBasis,
        QuestionStatus targetStatus
) {
}
