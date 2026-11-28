package com.seii.legal.function.controller;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.DocumentDtos;
import com.seii.legal.function.service.DocumentAppService;
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
@RequestMapping("/app/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentAppService documentAppService;

    @PostMapping("/upload")
    public AppResult<DocumentDtos.UploadDocumentResponse> upload(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody DocumentDtos.UploadDocumentRequest request) {
        return documentAppService.upload(credential, request);
    }

    @GetMapping("/list")
    public AppResult<PageResult<DocumentDtos.DocumentSummary>> list(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SystemType system,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return documentAppService.list(credential, keyword, system, page, size);
    }

    @GetMapping("/{id}")
    public AppResult<DocumentDtos.DocumentDetailResponse> detail(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String documentId) {
        return documentAppService.detail(credential, documentId);
    }

    @PostMapping("/{id}/case-info")
    public AppResult<Void> saveCaseInfo(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String documentId,
            @Valid @RequestBody DocumentDtos.SaveCaseInfoRequest request) {
        return documentAppService.saveCaseInfo(credential, documentId, request);
    }
}
