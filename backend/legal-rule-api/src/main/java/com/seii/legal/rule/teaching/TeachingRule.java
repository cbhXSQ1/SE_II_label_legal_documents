package com.seii.legal.rule.teaching;

import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.KnowledgePoint;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.Question;
import com.seii.legal.data.entity.QuestionVersion;
import java.time.LocalDateTime;
import java.util.List;

public interface TeachingRule {
    boolean uniquePointName(String name, List<KnowledgePoint> points);

    List<String> checkMaterialPublish(LearningMaterial material, String guideVersion, String classId);

    List<String> checkQuestion(String questionType, QuestionVersion version);

    int nextVersion(Question question);

    Score score(String answerContent, String standardAnswer, String questionType);

    double average(List<Double> scores);

    String completion(List<AnswerRecord> answers);

    KnowledgeStat summarize(List<AnswerRecord> answers, List<String> pointIds);

    List<String> weakPoints(KnowledgeStat stat, int threshold);

    List<Question> targetedQuestions(List<String> pointIds, List<Question> pool);

    long studyDuration(LocalDateTime first, LocalDateTime last, List<LocalDateTime> leaves);

    record Score(double total, List<DifferenceItem> differenceItems) {
    }

    record KnowledgeStat(List<String> pointIds, List<Integer> practiceCounts, List<Double> bestScores, List<Double> recentScores) {
    }
}
