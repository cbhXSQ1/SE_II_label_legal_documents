package com.seii.legal.function.support;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.repository.TaskRepository;
import org.springframework.stereotype.Component;

@Component
public class AdminTaskQueryStrategy implements TaskQueryStrategy {

    private final TaskRepository taskRepository;

    public AdminTaskQueryStrategy(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public IdentityType supportedIdentity() {
        return IdentityType.ADMIN;
    }

    @Override
    public PageResult<AnnotationTask> queryMyTasks(String accountId, SystemType system, int page, int size) {
        return taskRepository.findAll(system, page, size);
    }
}