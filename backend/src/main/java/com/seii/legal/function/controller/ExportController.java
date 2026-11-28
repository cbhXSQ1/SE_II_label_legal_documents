package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.ExportDtos;
import com.seii.legal.function.service.ExportAppService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportAppService exportAppService;

    @GetMapping("/tasks")
    public AppResult<ExportDtos.ExportableTasksResponse> tasks(
            @RequestHeader("Authorization") String credential) {
        return exportAppService.listExportableTasks(credential);
    }

    @PostMapping("/create")
    public AppResult<ExportDtos.CreateExportResponse> create(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody ExportDtos.CreateExportRequest request) {
        return exportAppService.createExport(credential, request);
    }

    @PostMapping("/import")
    public AppResult<ExportDtos.ImportExportResponse> importExport(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody ExportDtos.ImportExportRequest request) {
        return exportAppService.importExport(credential, request);
    }
}
