package com.seii.legal.function.controller;

import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.KnowledgePoint;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.Question;
import com.seii.legal.function.dto.teacher.ClassLearningView;
import com.seii.legal.function.dto.teacher.ExerciseCommand;
import com.seii.legal.function.dto.teacher.FeedbackCommand;
import com.seii.legal.function.dto.teacher.IdResult;
import com.seii.legal.function.dto.teacher.KnowledgePointCommand;
import com.seii.legal.function.dto.teacher.MaterialCommand;
import com.seii.legal.function.dto.teacher.MaterialStatusCommand;
import com.seii.legal.function.dto.teacher.PointDeleteCommand;
import com.seii.legal.function.dto.teacher.QuestionCommand;
import com.seii.legal.function.dto.teacher.QuestionResult;
import com.seii.legal.function.dto.teacher.TargetedView;
import com.seii.legal.function.dto.teacher.ThresholdCommand;
import com.seii.legal.function.service.TeacherAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/teacher")
public class TeacherController {

    private final TeacherAppService teacherAppService;

    public TeacherController(TeacherAppService teacherAppService) {
        this.teacherAppService = teacherAppService;
    }

    @GetMapping("/points")
    public AppResult<PageResult<KnowledgePoint>> points(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return teacherAppService.points(credential, category, page, size);
    }

    @PostMapping("/points")
    public AppResult<IdResult> savePoint(
            @RequestHeader("Authorization") String credential,
            @RequestBody KnowledgePointCommand command) {
        return teacherAppService.savePoint(credential, command);
    }

    @PostMapping("/points/delete")
    public AppResult<Void> deletePoint(
            @RequestHeader("Authorization") String credential,
            @RequestBody PointDeleteCommand command) {
        return teacherAppService.deletePoint(credential, command);
    }

    @GetMapping("/materials")
    public AppResult<PageResult<LearningMaterial>> materials(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return teacherAppService.materials(credential, status, page, size);
    }

    @PostMapping("/materials")
    public AppResult<IdResult> saveMaterial(
            @RequestHeader("Authorization") String credential,
            @RequestBody MaterialCommand command) {
        return teacherAppService.saveMaterial(credential, command);
    }

    @PostMapping("/materials/status")
    public AppResult<Void> updateMaterialStatus(
            @RequestHeader("Authorization") String credential,
            @RequestBody MaterialStatusCommand command) {
        return teacherAppService.updateMaterialStatus(credential, command);
    }

    @GetMapping("/questions")
    public AppResult<PageResult<Question>> questions(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "pointId", required = false) String pointId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return teacherAppService.questions(credential, pointId, page, size);
    }

    @PostMapping("/questions")
    public AppResult<QuestionResult> saveQuestion(
            @RequestHeader("Authorization") String credential,
            @RequestBody QuestionCommand command) {
        return teacherAppService.saveQuestion(credential, command);
    }

    @GetMapping("/exercises")
    public AppResult<PageResult<Exercise>> exercises(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return teacherAppService.exercises(credential, status, page, size);
    }

    @PostMapping("/exercises")
    public AppResult<IdResult> saveExercise(
            @RequestHeader("Authorization") String credential,
            @RequestBody ExerciseCommand command) {
        return teacherAppService.saveExercise(credential, command);
    }

    @GetMapping("/class-learning")
    public AppResult<ClassLearningView> classLearning(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam(value = "pointId", required = false) String pointId,
            @RequestParam(value = "exerciseId", required = false) String exerciseId) {
        return teacherAppService.classLearning(credential, classId, studentId, pointId, exerciseId);
    }

    @PostMapping("/threshold")
    public AppResult<Void> saveThreshold(
            @RequestHeader("Authorization") String credential,
            @RequestBody ThresholdCommand command) {
        return teacherAppService.saveThreshold(credential, command);
    }

    @GetMapping("/targeted")
    public AppResult<TargetedView> targeted(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "pointIds", required = false) String pointIds) {
        return teacherAppService.targeted(credential, classId, pointIds);
    }

    @PostMapping("/feedback")
    public AppResult<Void> feedback(
            @RequestHeader("Authorization") String credential,
            @RequestBody FeedbackCommand command) {
        return teacherAppService.feedback(credential, command);
    }
}
