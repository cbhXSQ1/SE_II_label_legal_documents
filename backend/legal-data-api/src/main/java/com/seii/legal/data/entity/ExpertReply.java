package com.seii.legal.data.entity;

import com.seii.legal.common.enums.QuestionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ExpertReply(
        String replyId,
        String questionId,
        String expertId,
        String content,
        LocalDateTime repliedAt
) {
}
