package com.seii.legal.function.dto;

import com.seii.legal.data.entity.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public final class TeacherDtos {

    private TeacherDtos() {
    }

    public record KnowledgePointItem(
            String id,
            String name,
            String category,
            String description,
            String difficulty,
            int materialCount,
            int questionCount
    ) {
    }

    public record SavePointRequest(
            String id,
            @NotBlank(message = "请填写知识点名称") String name,
            @NotBlank(message = "请选择知识点类别") String category,
            String description,
            String difficulty
    ) {
    }

    public record DeletePointRequest(
            @NotBlank(message = "知识点标识不能为空") String id
    ) {
    }

    public record IdResponse(String id) {
    }

    public record SaveMaterialRequest(
            String id,
            @NotBlank(message = "请填写材料标题") String title,
            @NotBlank(message = "请选择材料类型") String type,
            String content,
            String fileId,
            String guideVersion,
            List<String> pointIds,
            String classId,
            boolean publish
    ) {
    }

    public record MaterialStatusRequest(
            @NotBlank(message = "材料标识不能为空") String id,
            @NotBlank(message = "目标状态不能为空") String status
    ) {
    }

    public record SaveQuestionRequest(
            String id,
            String classId,
            @NotBlank(message = "请选择题型") String type,
            String stem,
            String documentId,
            String difficulty,
            String guideVersion,
            List<String> pointIds,
            List<String> options,
            String answer,
            Map<String, Object> standard
    ) {
    }

    public record SaveQuestionResponse(
            String id,
            int version
    ) {
    }

    public record SaveExerciseRequest(
            String id,
            @NotBlank(message = "请填写练习任务名称") String name,
            String description,
            String deadline,
            boolean allowRedo,
            String guideVersion,
            String classId,
            List<String> questionIds
    ) {
    }

    public record StudentLearningSummary(
            String id,
            String name,
            int materials,
            int exercises,
            List<Double> scores,
            List<String> weakPoints
    ) {
    }

    public record ClassLearningResponse(
            List<StudentLearningSummary> students,
            int threshold
    ) {
    }

    public record SaveThresholdRequest(
            @NotBlank(message = "班级标识不能为空") String classId,
            @NotNull(message = "请填写薄弱阈值") Integer threshold
    ) {
    }

    public record TargetedPracticeResponse(
            List<String> points,
            List<Question> questions
    ) {
    }

    public record TeacherFeedbackRequest(
            @NotBlank(message = "作答标识不能为空") String answerId,
            Double score,
            String feedback
    ) {
    }
}
