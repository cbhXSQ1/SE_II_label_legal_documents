package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnotatorQueryStrategy implements TaskQueryStrategy {

    private final TaskRepository taskRepository;

    @Override
    public IdentityType supportedIdentity() {
        return IdentityType.ANNOTATOR;
    }

    @Override
    public PageResult<AnnotationTask> queryMyTasks(String accountId, int page, int size) {
        return taskRepository.findByAnnotator(accountId, page, size);
    }
}
