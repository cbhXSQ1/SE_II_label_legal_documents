package com.seii.legal.function.support;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.repository.TaskRepository;
import org.springframework.stereotype.Component;

@Component
public class ManagerTaskQueryStrategy implements TaskQueryStrategy {

    private final TaskRepository taskRepository;

    public ManagerTaskQueryStrategy(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public IdentityType supportedIdentity() {
        return IdentityType.TASK_MANAGER;
    }

    @Override
    public PageResult<AnnotationTask> queryMyTasks(String accountId, int page, int size) {
        return taskRepository.findByManager(accountId, page, size);
    }
}