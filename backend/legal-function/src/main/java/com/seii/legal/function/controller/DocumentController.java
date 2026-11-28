package com.seii.legal.function.controller;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.document.CaseInfoView;
import com.seii.legal.function.dto.document.DocumentDetailView;
import com.seii.legal.function.dto.document.DocumentListItemView;
import com.seii.legal.function.dto.document.DocumentUploadResult;
import com.seii.legal.function.dto.document.SaveCaseInfoCommand;
import com.seii.legal.function.dto.document.UploadDocumentCommand;
import com.seii.legal.function.service.DocumentAppService;
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
public class DocumentController {

    private final DocumentAppService documentAppService;

    public DocumentController(DocumentAppService documentAppService) {
        this.documentAppService = documentAppService;
    }

    @PostMapping("/upload")
    public AppResult<DocumentUploadResult> upload(
            @RequestHeader("Authorization") String credential,
            @RequestBody UploadDocumentCommand command) {
        return documentAppService.upload(credential, command);
    }

    @GetMapping("/list")
    public AppResult<PageResult<DocumentListItemView>> list(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SystemType system,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return documentAppService.list(credential, keyword, system, page, size);
    }

    @GetMapping("/{id}")
    public AppResult<DocumentDetailView> detail(
            @RequestHeader("Authorization") String credential,
            @PathVariable String id) {
        return documentAppService.detail(credential, id);
    }

    @PostMapping("/{id}/case-info")
    public AppResult<Void> saveCaseInfo(
            @RequestHeader("Authorization") String credential,
            @PathVariable String id,
            @RequestBody SaveCaseInfoCommand command) {
        return documentAppService.saveCaseInfo(credential, id, command);
    }

    @GetMapping("/{id}/case-info")
    public AppResult<CaseInfoView> caseInfo(
            @RequestHeader("Authorization") String credential,
            @PathVariable String id) {
        return documentAppService.caseInfo(credential, id);
    }
}
