package com.seii.legal.function.controller;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.AdminDtos;
import com.seii.legal.function.service.AdminAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端接口控制器（ADMIN-01…07）。
 */
@RestController
@RequestMapping("/app/admin")
public class AdminController {

    private final AdminAppService adminAppService;

    public AdminController(AdminAppService adminAppService) {
        this.adminAppService = adminAppService;
    }

    @GetMapping("/applications")
    public AppResult<PageResult<AdminDtos.ApplicationItem>> applications(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return adminAppService.listApplications(credential, status, page, size);
    }

    @PostMapping("/applications/review")
    public AppResult<Void> reviewApplication(
            @RequestHeader("Authorization") String credential,
            @RequestBody AdminDtos.ReviewApplicationRequest request) {
        return adminAppService.reviewApplication(credential, request);
    }

    @GetMapping("/accounts")
    public AppResult<PageResult<AdminDtos.AccountItem>> accounts(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return adminAppService.listAccounts(credential, keyword, page, size);
    }

    @PostMapping("/accounts/roles")
    public AppResult<Void> updateAccountRoles(
            @RequestHeader("Authorization") String credential,
            @RequestBody AdminDtos.UpdateAccountRolesRequest request) {
        return adminAppService.updateAccountRoles(credential, request);
    }

    @PostMapping("/guide/import")
    public AppResult<AdminDtos.ImportGuideResponse> importGuide(
            @RequestHeader("Authorization") String credential,
            @RequestBody AdminDtos.ImportGuideRequest request) {
        return adminAppService.importGuide(credential, request);
    }

    @GetMapping("/guides")
    public AppResult<AdminDtos.GuideListResponse> guides(
            @RequestHeader("Authorization") String credential) {
        return adminAppService.listGuides(credential);
    }

    @GetMapping("/overview")
    public AppResult<AdminDtos.OverviewResponse> overview(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) SystemType system,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return adminAppService.overview(credential, system, page, size);
    }
}
