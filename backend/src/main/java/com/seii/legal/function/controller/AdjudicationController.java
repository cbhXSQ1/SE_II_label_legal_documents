package com.seii.legal.function.controller;

import com.seii.legal.common.enums.QuestionStatus;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.function.dto.AdjudicationDtos;
import com.seii.legal.function.service.AdjudicationAppService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/adjudication")
@RequiredArgsConstructor
public class AdjudicationController {

    private final AdjudicationAppService adjudicationAppService;

    @PostMapping("/compare")
    public AppResult<AdjudicationDtos.CompareResponse> compare(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AdjudicationDtos.CompareRequest request) {
        return adjudicationAppService.compare(credential, request);
    }

    @GetMapping("/conflicts")
    public AppResult<AdjudicationDtos.ConflictsResponse> conflicts(
            @RequestHeader("Authorization") String credential,
            @RequestParam String taskId,
            @RequestParam String documentId,
            @RequestParam RoundType round) {
        return adjudicationAppService.conflicts(credential, taskId, documentId, round);
    }

    @PostMapping("/decide")
    public AppResult<Void> decide(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AdjudicationDtos.DecideRequest request) {
        return adjudicationAppService.decide(credential, request);
    }

    @PostMapping("/expert-question")
    public AppResult<AdjudicationDtos.ExpertQuestionResponse> expertQuestion(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AdjudicationDtos.ExpertQuestionRequest request) {
        return adjudicationAppService.submitExpertQuestion(credential, request);
    }

    @GetMapping("/expert-questions")
    public AppResult<PageResult<ExpertQuestion>> expertQuestions(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return adjudicationAppService.listExpertQuestions(credential, status, page, size);
    }

    @PostMapping("/expert-reply")
    public AppResult<Void> expertReply(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AdjudicationDtos.ExpertReplyRequest request) {
        return adjudicationAppService.replyExpertQuestion(credential, request);
    }

    @PostMapping("/diagram-review")
    public AppResult<Void> diagramReview(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AdjudicationDtos.DiagramReviewRequest request) {
        return adjudicationAppService.reviewDiagram(credential, request);
    }

    @GetMapping("/final")
    public AppResult<AdjudicationDtos.FinalVersionResponse> viewFinal(
            @RequestHeader("Authorization") String credential,
            @RequestParam String taskId,
            @RequestParam String documentId,
            @RequestParam(required = false) VersionType versionType) {
        return adjudicationAppService.viewFinalVersion(credential, taskId, documentId, versionType);
    }

    @PostMapping("/final-confirm")
    public AppResult<Void> finalConfirm(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AdjudicationDtos.FinalConfirmRequest request) {
        return adjudicationAppService.confirmFinalVersion(credential, request);
    }
}
