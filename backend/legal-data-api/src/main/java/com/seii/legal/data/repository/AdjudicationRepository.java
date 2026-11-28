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

    List<AdjudicationRecord> findDecisionsByRound(String roundId);

    String saveExpertQuestion(ExpertQuestion question);

    Optional<ExpertQuestion> findExpertQuestion(String questionId);

    PageResult<ExpertQuestion> findExpertQuestions(String expertId, QuestionStatus status, int page, int size);

    String saveExpertReply(ExpertReply reply);

    void updateQuestionStatus(String questionId, QuestionStatus status);

    List<DiagramReview> findDiagramReviews(String taskId, String documentId);

    void saveDiagramReview(DiagramReview review);

    String saveFinalVersion(FinalVersion version);

    Optional<FinalVersion> findFinalVersion(String taskId, String documentId);

    Optional<FinalVersion> findFinalVersion(String taskId, String documentId, VersionType versionType);

    Optional<FinalVersion> findFinalVersionByRound(String roundId, VersionType versionType);

    void confirmTask(String taskId, String confirmerId, LocalDateTime time);
}
