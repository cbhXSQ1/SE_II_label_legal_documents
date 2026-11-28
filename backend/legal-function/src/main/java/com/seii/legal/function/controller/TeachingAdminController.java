package com.seii.legal.function.controller;

import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.AccountApplication;
import com.seii.legal.function.dto.teachingadmin.AccountQuery;
import com.seii.legal.function.dto.teachingadmin.AccountView;
import com.seii.legal.function.dto.teachingadmin.ApplicationQuery;
import com.seii.legal.function.dto.teachingadmin.ClassQuery;
import com.seii.legal.function.dto.teachingadmin.ClassView;
import com.seii.legal.function.dto.teachingadmin.ImportCommand;
import com.seii.legal.function.dto.teachingadmin.ImportResult;
import com.seii.legal.function.dto.teachingadmin.ImportResultView;
import com.seii.legal.function.dto.teachingadmin.ReviewCommand;
import com.seii.legal.function.service.TeachingAdminAppService;
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
public class TeachingAdminController {

    private final TeachingAdminAppService teachingAdminAppService;

    public TeachingAdminController(TeachingAdminAppService teachingAdminAppService) {
        this.teachingAdminAppService = teachingAdminAppService;
    }

    @PostMapping("/import")
    public AppResult<ImportResult> importClasses(
            @RequestHeader("Authorization") String credential,
            @RequestBody ImportCommand command) {
        return teachingAdminAppService.importClasses(credential, command);
    }

    @GetMapping("/import/{id}/result")
    public AppResult<ImportResultView> importResult(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String importId) {
        return teachingAdminAppService.importResult(credential, importId);
    }

    @GetMapping("/classes")
    public AppResult<PageResult<ClassView>> classes(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teachingAdminAppService.listClasses(credential, new ClassQuery(term, teacherId, page, size));
    }

    @GetMapping("/accounts")
    public AppResult<PageResult<AccountView>> accounts(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teachingAdminAppService.listAccounts(credential,
                new AccountQuery(classId, status, keyword, page, size));
    }

    @GetMapping("/applications")
    public AppResult<PageResult<AccountApplication>> applications(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return teachingAdminAppService.listApplications(credential, new ApplicationQuery(status, page, size));
    }

    @PostMapping("/applications/review")
    public AppResult<Void> review(
            @RequestHeader("Authorization") String credential,
            @RequestBody ReviewCommand command) {
        return teachingAdminAppService.reviewApplication(credential, command);
    }
}
