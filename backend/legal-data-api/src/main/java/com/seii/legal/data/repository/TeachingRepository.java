package com.seii.legal.data.repository;

import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.KnowledgePoint;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.LearningRecord;
import com.seii.legal.data.entity.LearningStat;
import com.seii.legal.data.entity.Question;
import com.seii.legal.data.entity.QuestionVersion;
import com.seii.legal.data.entity.TeacherFeedback;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeachingRepository {
    String saveKnowledgePoint(KnowledgePoint point);

    Optional<KnowledgePoint> findKnowledgePoint(String pointId);

    void deleteKnowledgePoint(String pointId);

    PageResult<KnowledgePoint> findKnowledgePoints(String teacherId, String category, int page, int size);

    String saveMaterial(LearningMaterial material);

    Optional<LearningMaterial> findMaterial(String materialId);

    void updateMaterialStatus(String materialId, String status, String operatorId, LocalDateTime time);

    PageResult<LearningMaterial> findMaterials(String classId, String status, int page, int size);

    void saveLearningRecord(LearningRecord record);

    Optional<LearningRecord> findLearningRecord(String studentId, String materialId);

    String saveQuestion(Question question);

    Optional<Question> findQuestion(String questionId);

    int saveQuestionVersion(String questionId, int version, String standardAnswer, String guideVersion, LocalDateTime effectiveAt);

    Optional<QuestionVersion> findQuestionVersion(String questionId, int version);

    PageResult<Question> findQuestions(String classId, String pointId, int page, int size);

    String saveExercise(Exercise exercise);

    Optional<Exercise> findExercise(String exerciseId);

    void updateExerciseStatus(String exerciseId, String status, String operatorId, LocalDateTime time);

    PageResult<Exercise> findExercises(String classId, String status, int page, int size);

    String saveAnswer(AnswerRecord answer, String requestId);

    void saveAnswerDraft(AnswerRecord draft);

    Optional<AnswerRecord> findAnswerDraft(String studentId, String exerciseId);

    void deleteAnswerDraft(String studentId, String exerciseId);

    Optional<AnswerRecord> findAnswer(String answerId);

    List<AnswerRecord> findAnswers(String studentId, String exerciseId);

    void updateAnswerStatus(String answerId, String status, String feedback, String teacherId, LocalDateTime time);

    void updateAnswerScore(String answerId, double score);

    void saveDifference(String answerId, List<DifferenceItem> items);

    List<DifferenceItem> findDifferences(String answerId, String questionId);

    void saveTeacherFeedback(String answerId, String teacherId, String content, LocalDateTime time);

    Optional<TeacherFeedback> findTeacherFeedback(String answerId);

    LearningStat findLearningStat(String studentId, String classId, String pointId, String exerciseId, LocalDateTime from, LocalDateTime to);

    void saveWeakThreshold(String classId, int threshold);

    Optional<Integer> findWeakThreshold(String classId);
}
