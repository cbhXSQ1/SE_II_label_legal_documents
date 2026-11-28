package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.student.AnswerDetailView;
import com.seii.legal.function.dto.student.AnswerDraftCommand;
import com.seii.legal.function.dto.student.AnswerDraftResult;
import com.seii.legal.function.dto.student.AnswerSubmitCommand;
import com.seii.legal.function.dto.student.LearningView;
import com.seii.legal.function.dto.student.StudentAnswersView;
import com.seii.legal.function.dto.student.StudentExerciseDetailView;
import com.seii.legal.function.dto.student.StudentExercisesView;
import com.seii.legal.function.dto.student.StudentMaterialsView;
import com.seii.legal.function.dto.student.SubmitAnswerResult;
import com.seii.legal.function.service.StudentAppService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
public class StudentController {

    private final StudentAppService studentAppService;

    public StudentController(StudentAppService studentAppService) {
        this.studentAppService = studentAppService;
    }

    @GetMapping("/materials")
    public AppResult<StudentMaterialsView> materials(
            @RequestHeader("Authorization") String credential) {
        return studentAppService.materials(credential);
    }

    @GetMapping("/exercises")
    public AppResult<StudentExercisesView> exercises(
            @RequestHeader("Authorization") String credential) {
        return studentAppService.exercises(credential);
    }

    @GetMapping("/exercise/{id}")
    public AppResult<StudentExerciseDetailView> exerciseDetail(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String exerciseId) {
        return studentAppService.exerciseDetail(credential, exerciseId);
    }

    @PostMapping("/answer/draft")
    public AppResult<AnswerDraftResult> saveDraft(
            @RequestHeader("Authorization") String credential,
            @RequestBody AnswerDraftCommand command) {
        return studentAppService.saveDraft(credential, command);
    }

    @PostMapping("/answer/submit")
    public AppResult<SubmitAnswerResult> submit(
            @RequestHeader("Authorization") String credential,
            @RequestBody AnswerSubmitCommand command) {
        return studentAppService.submit(credential, command);
    }

    @GetMapping("/answers")
    public AppResult<StudentAnswersView> answers(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "exerciseId", required = false) String exerciseId) {
        return studentAppService.answers(credential, exerciseId);
    }

    @GetMapping("/answer/{id}")
    public AppResult<AnswerDetailView> answerDetail(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String answerId) {
        return studentAppService.answerDetail(credential, answerId);
    }

    @GetMapping("/learning")
    public AppResult<LearningView> learning(
            @RequestHeader("Authorization") String credential,
            @RequestParam(value = "pointId", required = false) String pointId,
            @RequestParam(value = "exerciseId", required = false) String exerciseId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        return studentAppService.learning(credential, pointId, exerciseId, from, to);
    }
}
