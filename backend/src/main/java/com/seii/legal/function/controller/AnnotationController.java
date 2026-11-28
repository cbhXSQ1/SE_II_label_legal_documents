package com.seii.legal.function.controller;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.AnnotationDtos;
import com.seii.legal.function.service.AnnotationAppService;
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
@RequestMapping("/app/annotation")
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationAppService annotationAppService;

    @GetMapping("/guide")
    public AppResult<AnnotationDtos.TaskGuideResponse> guide(
            @RequestHeader("Authorization") String credential,
            @RequestParam String taskId) {
        return annotationAppService.taskGuide(credential, taskId);
    }

    @PostMapping("/confirm-guide")
    public AppResult<Void> confirmGuide(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AnnotationDtos.ConfirmGuideRequest request) {
        return annotationAppService.confirmGuide(credential, request);
    }

    @GetMapping("/load")
    public AppResult<AnnotationDtos.LoadAnnotationResponse> load(
            @RequestHeader("Authorization") String credential,
            @RequestParam String taskId,
            @RequestParam String documentId,
            @RequestParam(required = false) RoundType round) {
        return annotationAppService.load(credential, taskId, documentId, round);
    }

    @PostMapping("/draft")
    public AppResult<AnnotationDtos.SaveDraftResponse> saveDraft(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AnnotationDtos.SaveDraftRequest request) {
        return annotationAppService.saveDraft(credential, request);
    }

    @PostMapping("/submit")
    public AppResult<AnnotationDtos.SubmitAnnotationResponse> submit(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AnnotationDtos.SubmitAnnotationRequest request) {
        return annotationAppService.submit(credential, request);
    }
}
