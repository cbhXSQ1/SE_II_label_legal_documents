package com.seii.legal.function.controller;

import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.Question;
import com.seii.legal.function.dto.TeacherDtos;
import com.seii.legal.function.service.TeacherAppService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherAppService teacherAppService;

    @GetMapping("/points")
    public AppResult<PageResult<TeacherDtos.KnowledgePointItem>> points(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teacherAppService.listPoints(credential, category, page, size);
    }

    @PostMapping("/points")
    public AppResult<TeacherDtos.IdResponse> savePoint(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.SavePointRequest request) {
        return teacherAppService.savePoint(credential, request);
    }

    @PostMapping("/points/delete")
    public AppResult<Void> deletePoint(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.DeletePointRequest request) {
        return teacherAppService.deletePoint(credential, request);
    }

    @GetMapping("/materials")
    public AppResult<PageResult<LearningMaterial>> materials(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teacherAppService.listMaterials(credential, classId, status, page, size);
    }

    @PostMapping("/materials")
    public AppResult<TeacherDtos.IdResponse> saveMaterial(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.SaveMaterialRequest request) {
        return teacherAppService.saveMaterial(credential, request);
    }

    @PostMapping("/materials/status")
    public AppResult<Void> materialStatus(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.MaterialStatusRequest request) {
        return teacherAppService.updateMaterialStatus(credential, request);
    }

    @GetMapping("/questions")
    public AppResult<PageResult<Question>> questions(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String pointId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teacherAppService.listQuestions(credential, classId, pointId, page, size);
    }

    @PostMapping("/questions")
    public AppResult<TeacherDtos.SaveQuestionResponse> saveQuestion(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.SaveQuestionRequest request) {
        return teacherAppService.saveQuestion(credential, request);
    }

    @GetMapping("/exercises")
    public AppResult<PageResult<Exercise>> exercises(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teacherAppService.listExercises(credential, classId, status, page, size);
    }

    @PostMapping("/exercises")
    public AppResult<TeacherDtos.IdResponse> saveExercise(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.SaveExerciseRequest request) {
        return teacherAppService.saveExercise(credential, request);
    }

    @GetMapping("/class-learning")
    public AppResult<TeacherDtos.ClassLearningResponse> classLearning(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String pointId,
            @RequestParam(required = false) String exerciseId) {
        return teacherAppService.classLearning(credential, classId, studentId, pointId, exerciseId);
    }

    @PostMapping("/threshold")
    public AppResult<Void> threshold(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.SaveThresholdRequest request) {
        return teacherAppService.saveThreshold(credential, request);
    }

    @GetMapping("/targeted")
    public AppResult<TeacherDtos.TargetedPracticeResponse> targeted(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) List<String> pointIds) {
        return teacherAppService.targetedPractice(credential, classId, pointIds);
    }

    @PostMapping("/feedback")
    public AppResult<Void> feedback(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeacherDtos.TeacherFeedbackRequest request) {
        return teacherAppService.feedback(credential, request);
    }
}
