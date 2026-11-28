package com.seii.legal.function.controller;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.task.CreateTaskCommand;
import com.seii.legal.function.dto.task.ReopenTaskCommand;
import com.seii.legal.function.dto.task.TaskDetailView;
import com.seii.legal.function.dto.task.TaskIdView;
import com.seii.legal.function.dto.task.TaskListView;
import com.seii.legal.function.dto.task.TaskProgressView;
import com.seii.legal.function.dto.task.UpdateTaskCommand;
import com.seii.legal.function.dto.task.WithdrawTaskCommand;
import com.seii.legal.function.service.TaskAppService;
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
public class TaskController {

    private final TaskAppService taskAppService;

    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    @PostMapping("/create")
    public AppResult<TaskIdView> create(@RequestHeader("Authorization") String credential,
                                        @RequestBody CreateTaskCommand command) {
        return taskAppService.create(credential, command);
    }

    @PostMapping("/{id}/publish")
    public AppResult<Void> publish(@RequestHeader("Authorization") String credential,
                                   @PathVariable("id") String taskId) {
        return taskAppService.publish(credential, taskId);
    }

    @PostMapping("/{id}/update")
    public AppResult<Void> update(@RequestHeader("Authorization") String credential,
                                  @PathVariable("id") String taskId,
                                  @RequestBody UpdateTaskCommand command) {
        return taskAppService.update(credential, taskId, command);
    }

    @PostMapping("/{id}/withdraw")
    public AppResult<Void> withdraw(@RequestHeader("Authorization") String credential,
                                    @PathVariable("id") String taskId,
                                    @RequestBody WithdrawTaskCommand command) {
        return taskAppService.withdraw(credential, taskId, command);
    }

    @PostMapping("/{id}/reopen")
    public AppResult<Void> reopen(@RequestHeader("Authorization") String credential,
                                  @PathVariable("id") String taskId,
                                  @RequestBody ReopenTaskCommand command) {
        return taskAppService.reopen(credential, taskId, command);
    }

    @GetMapping("/my")
    public AppResult<TaskListView> myTasks(@RequestHeader("Authorization") String credential,
                                          @RequestParam(value = "system", required = false) SystemType system,
                                          @RequestParam(value = "page", defaultValue = "1") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size) {
        return taskAppService.myTasks(credential, system, page, size);
    }

    @GetMapping("/{id}")
    public AppResult<TaskDetailView> detail(@RequestHeader("Authorization") String credential,
                                           @PathVariable("id") String taskId) {
        return taskAppService.detail(credential, taskId);
    }

    @GetMapping("/{id}/progress")
    public AppResult<TaskProgressView> progress(@RequestHeader("Authorization") String credential,
                                               @PathVariable("id") String taskId) {
        return taskAppService.progress(credential, taskId);
    }
}
