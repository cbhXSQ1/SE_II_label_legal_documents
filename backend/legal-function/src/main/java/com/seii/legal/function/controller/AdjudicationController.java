package com.seii.legal.function.controller;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.adjudication.CompareCommand;
import com.seii.legal.function.dto.adjudication.CompareResult;
import com.seii.legal.function.dto.adjudication.ConflictListView;
import com.seii.legal.function.dto.adjudication.ConflictQuery;
import com.seii.legal.function.dto.adjudication.DecideCommand;
import com.seii.legal.function.dto.adjudication.DiagramReviewCommand;
import com.seii.legal.function.dto.adjudication.ExpertQuestionCommand;
import com.seii.legal.function.dto.adjudication.ExpertQuestionCreated;
import com.seii.legal.function.dto.adjudication.ExpertQuestionView;
import com.seii.legal.function.dto.adjudication.ExpertReplyCommand;
import com.seii.legal.function.dto.adjudication.FinalConfirmCommand;
import com.seii.legal.function.dto.adjudication.FinalQuery;
import com.seii.legal.function.dto.adjudication.FinalVersionView;
import com.seii.legal.function.service.AdjudicationAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/adjudication")
public class AdjudicationController {

    private final AdjudicationAppService adjudicationAppService;

    public AdjudicationController(AdjudicationAppService adjudicationAppService) {
        this.adjudicationAppService = adjudicationAppService;
    }

    @PostMapping("/compare")
    public AppResult<CompareResult> compare(
            @RequestHeader("Authorization") String credential,
            @RequestBody CompareCommand command) {
        return adjudicationAppService.compare(credential, command);
    }

    @GetMapping("/conflicts")
    public AppResult<ConflictListView> conflicts(
            @RequestHeader("Authorization") String credential,
            @RequestParam String taskId,
            @RequestParam String documentId,
            @RequestParam RoundType round) {
        return adjudicationAppService.conflicts(credential, new ConflictQuery(taskId, documentId, round));
    }

    @PostMapping("/decide")
    public AppResult<Void> decide(
            @RequestHeader("Authorization") String credential,
            @RequestBody DecideCommand command) {
        return adjudicationAppService.decide(credential, command);
    }

    @PostMapping("/expert-question")
    public AppResult<ExpertQuestionCreated> expertQuestion(
            @RequestHeader("Authorization") String credential,
            @RequestBody ExpertQuestionCommand command) {
        return adjudicationAppService.askExpert(credential, command);
    }

    @GetMapping("/expert-questions")
    public AppResult<PageResult<ExpertQuestionView>> expertQuestions(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return adjudicationAppService.expertQuestions(credential, status, page, size);
    }

    @PostMapping("/expert-reply")
    public AppResult<Void> expertReply(
            @RequestHeader("Authorization") String credential,
            @RequestBody ExpertReplyCommand command) {
        return adjudicationAppService.replyExpert(credential, command);
    }

    @PostMapping("/diagram-review")
    public AppResult<Void> diagramReview(
            @RequestHeader("Authorization") String credential,
            @RequestBody DiagramReviewCommand command) {
        return adjudicationAppService.reviewDiagram(credential, command);
    }

    @GetMapping("/final")
    public AppResult<FinalVersionView> finalVersion(
            @RequestHeader("Authorization") String credential,
            @RequestParam String taskId,
            @RequestParam String documentId) {
        return adjudicationAppService.finalVersion(credential, new FinalQuery(taskId, documentId));
    }

    @PostMapping("/final-confirm")
    public AppResult<Void> finalConfirm(
            @RequestHeader("Authorization") String credential,
            @RequestBody FinalConfirmCommand command) {
        return adjudicationAppService.finalConfirm(credential, command);
    }
}
