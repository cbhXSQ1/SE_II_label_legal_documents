package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.export.ExportCommand;
import com.seii.legal.function.dto.export.ExportResult;
import com.seii.legal.function.dto.export.ExportTaskListView;
import com.seii.legal.function.dto.export.ImportCommand;
import com.seii.legal.function.dto.export.ImportResult;
import com.seii.legal.function.service.ExportAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/export")
public class ExportController {

    private final ExportAppService exportAppService;

    public ExportController(ExportAppService exportAppService) {
        this.exportAppService = exportAppService;
    }

    @GetMapping("/tasks")
    public AppResult<ExportTaskListView> tasks(@RequestHeader("Authorization") String credential) {
        return exportAppService.exportableTasks(credential);
    }

    @PostMapping("/create")
    public AppResult<ExportResult> create(
            @RequestHeader("Authorization") String credential,
            @RequestBody ExportCommand command) {
        return exportAppService.create(credential, command);
    }

    @PostMapping("/import")
    public AppResult<ImportResult> importFile(
            @RequestHeader("Authorization") String credential,
            @RequestBody ImportCommand command) {
        return exportAppService.importFile(credential, command);
    }
}
