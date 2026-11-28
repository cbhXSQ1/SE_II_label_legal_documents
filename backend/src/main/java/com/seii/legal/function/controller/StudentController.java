package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.StudentDtos;
import com.seii.legal.function.service.StudentAppService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentAppService studentAppService;

    @GetMapping("/materials")
    public AppResult<StudentDtos.StudentMaterialsResponse> materials(
            @RequestHeader("Authorization") String credential) {
        return studentAppService.materials(credential);
    }

    @GetMapping("/exercises")
    public AppResult<StudentDtos.StudentExercisesResponse> exercises(
            @RequestHeader("Authorization") String credential) {
        return studentAppService.exercises(credential);
    }

    @GetMapping("/exercise/{id}")
    public AppResult<StudentDtos.ExerciseDetailResponse> exerciseDetail(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String exerciseId) {
        return studentAppService.exerciseDetail(credential, exerciseId);
    }

    @PostMapping("/answer/draft")
    public AppResult<StudentDtos.SaveAnswerDraftResponse> saveDraft(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody StudentDtos.SaveAnswerDraftRequest request) {
        return studentAppService.saveAnswerDraft(credential, request);
    }

    @PostMapping("/answer/submit")
    public AppResult<StudentDtos.SubmitAnswerResponse> submitAnswer(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody StudentDtos.SubmitAnswerRequest request) {
        return studentAppService.submitAnswer(credential, request);
    }

    @GetMapping("/answers")
    public AppResult<StudentDtos.AnswerListResponse> answers(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String exerciseId) {
        return studentAppService.listAnswers(credential, exerciseId);
    }

    @GetMapping("/answer/{id}")
    public AppResult<StudentDtos.AnswerDetailResponse> answerDetail(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String answerId) {
        return studentAppService.answerDetail(credential, answerId);
    }

    @GetMapping("/learning")
    public AppResult<StudentDtos.StudentLearningResponse> learning(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String pointId,
            @RequestParam(required = false) String exerciseId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return studentAppService.learning(credential, pointId, exerciseId, from, to);
    }
}
