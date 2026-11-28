package com.seii.legal.data.repository;

import com.seii.legal.common.enums.QuestionStatus;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.data.entity.ExpertReply;
import com.seii.legal.data.entity.FinalVersion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdjudicationRepository {
    String saveDecision(AdjudicationRecord record);

    List<AdjudicationRecord> findDecisions(String taskId, String documentId, RoundType round);

    String saveExpertQuestion(ExpertQuestion question);

    /** 附录协同项：专家回复后查询提交仲裁员发送通知。 */
    Optional<ExpertQuestion> findExpertQuestion(String questionId);

    PageResult<ExpertQuestion> findExpertQuestions(String expertId, QuestionStatus status, int page, int size);

    String saveExpertReply(ExpertReply reply);

    void updateQuestionStatus(String questionId, QuestionStatus status);

    void saveDiagramReview(DiagramReview review);

    /** 附录协同项：确认最终版本前查询图示审查结果。 */
    List<DiagramReview> findDiagramReviews(String taskId, String documentId);

    String saveFinalVersion(FinalVersion version);

    Optional<FinalVersion> findFinalVersion(String taskId, String documentId);

    Optional<FinalVersion> findFinalVersion(String taskId, String documentId, VersionType versionType);

    void confirmTask(String taskId, String confirmerId, LocalDateTime time);
}
