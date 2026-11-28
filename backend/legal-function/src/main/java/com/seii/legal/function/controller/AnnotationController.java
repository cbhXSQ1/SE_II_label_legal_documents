package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.annotation.LoadAnnotationView;
import com.seii.legal.function.dto.annotation.SaveDraftCommand;
import com.seii.legal.function.dto.annotation.SaveDraftView;
import com.seii.legal.function.dto.annotation.SubmitCommand;
import com.seii.legal.function.dto.annotation.SubmitView;
import com.seii.legal.function.service.AnnotationAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/annotation")
public class AnnotationController {

    private final AnnotationAppService annotationAppService;

    public AnnotationController(AnnotationAppService annotationAppService) {
        this.annotationAppService = annotationAppService;
    }

    @GetMapping("/load")
    public AppResult<LoadAnnotationView> load(
            @RequestHeader(value = "Authorization", required = false) String credential,
            @RequestParam String taskId,
            @RequestParam String documentId) {
        return annotationAppService.load(credential, taskId, documentId);
    }

    @PostMapping("/draft")
    public AppResult<SaveDraftView> saveDraft(
            @RequestHeader(value = "Authorization", required = false) String credential,
            @RequestBody SaveDraftCommand command) {
        return annotationAppService.saveDraft(credential, command);
    }

    @PostMapping("/submit")
    public AppResult<SubmitView> submit(
            @RequestHeader(value = "Authorization", required = false) String credential,
            @RequestBody SubmitCommand command) {
        return annotationAppService.submit(credential, command);
    }
}
