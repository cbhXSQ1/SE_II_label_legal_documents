package com.seii.legal.function.controller;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.function.dto.TeachingAdminDtos;
import com.seii.legal.function.service.TeachingAdminAppService;
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
@RequestMapping("/app/teaching-admin")
@RequiredArgsConstructor
public class TeachingAdminController {

    private final TeachingAdminAppService teachingAdminAppService;

    @PostMapping("/import")
    public AppResult<TeachingAdminDtos.ImportRosterResponse> importRoster(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeachingAdminDtos.ImportRosterRequest request) {
        return teachingAdminAppService.importRoster(credential, request);
    }

    @GetMapping("/import/{id}/result")
    public AppResult<TeachingAdminDtos.ImportResultResponse> importResult(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String importId) {
        return teachingAdminAppService.importResult(credential, importId);
    }

    @GetMapping("/classes")
    public AppResult<PageResult<TeachingAdminDtos.ClassItem>> classes(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teachingAdminAppService.listClasses(credential, term, teacherId, page, size);
    }

    @GetMapping("/accounts")
    public AppResult<PageResult<TeachingAdminDtos.TeachingAccountItem>> accounts(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teachingAdminAppService.listAccounts(credential, classId, status, keyword, page, size);
    }

    @GetMapping("/applications")
    public AppResult<PageResult<AccountApplication>> applications(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teachingAdminAppService.listApplications(credential, status, page, size);
    }

    @PostMapping("/applications/review")
    public AppResult<Void> reviewApplication(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TeachingAdminDtos.ReviewTeachingApplicationRequest request) {
        return teachingAdminAppService.reviewApplication(credential, request);
    }
}
