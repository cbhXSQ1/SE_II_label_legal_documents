package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.TaskDtos;
import com.seii.legal.function.service.TaskAppService;
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
@RequestMapping("/app/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskAppService taskAppService;

    @PostMapping("/create")
    public AppResult<TaskDtos.CreateTaskResponse> create(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody TaskDtos.CreateTaskRequest request) {
        return taskAppService.create(credential, request);
    }

    @PostMapping("/{id}/publish")
    public AppResult<Void> publish(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String taskId) {
        return taskAppService.publish(credential, taskId);
    }

    @PostMapping("/{id}/update")
    public AppResult<Void> update(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String taskId,
            @RequestBody TaskDtos.UpdateTaskRequest request) {
        return taskAppService.update(credential, taskId, request);
    }

    @PostMapping("/{id}/withdraw")
    public AppResult<Void> withdraw(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String taskId,
            @Valid @RequestBody TaskDtos.WithdrawTaskRequest request) {
        return taskAppService.withdraw(credential, taskId, request);
    }

    @PostMapping("/{id}/reopen")
    public AppResult<Void> reopen(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String taskId,
            @Valid @RequestBody TaskDtos.ReopenTaskRequest request) {
        return taskAppService.reopen(credential, taskId, request);
    }

    @GetMapping("/my")
    public AppResult<TaskDtos.MyTasksResponse> myTasks(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return taskAppService.myTasks(credential, page, size);
    }

    @GetMapping("/{id}")
    public AppResult<TaskDtos.TaskDetailResponse> detail(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String taskId) {
        return taskAppService.detail(credential, taskId);
    }

    @GetMapping("/{id}/progress")
    public AppResult<TaskDtos.TaskProgressResponse> progress(
            @RequestHeader("Authorization") String credential,
            @PathVariable("id") String taskId) {
        return taskAppService.progress(credential, taskId);
    }
}
