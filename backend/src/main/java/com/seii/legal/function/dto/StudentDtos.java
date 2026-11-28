package com.seii.legal.function.dto;

import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.LearningStat;
import com.seii.legal.data.entity.Question;
import com.seii.legal.data.entity.TeacherFeedback;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class StudentDtos {

    private StudentDtos() {
    }

    public record StudentMaterialItem(
            LearningMaterial material,
            String completionStatus,
            long durationSeconds,
            boolean confirmed
    ) {
    }

    public record StudentMaterialsResponse(List<StudentMaterialItem> materials) {
    }

    public record StudentExerciseItem(
            Exercise exercise,
            String completionStatus,
            Long remainingSeconds
    ) {
    }

    public record StudentExercisesResponse(List<StudentExerciseItem> exercises) {
    }

    public record ExerciseDetailResponse(
            String id,
            String name,
            String deadline,
            boolean allowRedo,
            List<Question> questions
    ) {
    }

    public record QuestionAnswerItem(
            String questionId,
            String content
    ) {
    }

    public record SaveAnswerDraftRequest(
            @NotBlank(message = "练习任务标识不能为空") String exerciseId,
            List<QuestionAnswerItem> answers
    ) {
    }

    public record SaveAnswerDraftResponse(String savedAt) {
    }

    public record SubmitAnswerRequest(
            @NotBlank(message = "练习任务标识不能为空") String exerciseId,
            List<QuestionAnswerItem> answers,
            String requestId
    ) {
    }

    public record SubmitAnswerResponse(
            String answerId,
            double score,
            String status,
            List<DifferenceItem> differences,
            String systemFeedback
    ) {
    }

    public record AnswerListResponse(List<AnswerRecord> answers) {
    }

    public record AnswerDetailResponse(
            AnswerRecord answer,
            List<DifferenceItem> differences,
            String systemFeedback,
            TeacherFeedback teacherFeedback
    ) {
    }

    public record StudentLearningResponse(
            List<StudentMaterialItem> materials,
            List<AnswerRecord> answers,
            List<LearningStat> stats
    ) {
    }
}
