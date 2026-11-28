package com.seii.legal.data.repository;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.TaskAssignment;
import com.seii.legal.data.entity.TaskChangeLog;
import com.seii.legal.data.entity.TaskProgress;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    String save(AnnotationTask task);

    Optional<AnnotationTask> findById(String taskId);

    void update(String taskId, AnnotationTask changes, String operatorId, LocalDateTime time);

    void updateStatus(String taskId, TaskStatus status, String operatorId, LocalDateTime time);

    void saveAssignment(String taskId, List<String> annotatorIds, String arbiterId, LocalDateTime time);

    Optional<TaskAssignment> findAssignment(String taskId);

    PageResult<AnnotationTask> findByManager(String managerId, int page, int size);

    PageResult<AnnotationTask> findByArbiter(String arbiterId, int page, int size);

    PageResult<AnnotationTask> findByAnnotator(String annotatorId, int page, int size);

    PageResult<AnnotationTask> findAll(SystemType system, int page, int size);

    TaskProgress findProgress(String taskId);

    void saveChangeLog(TaskChangeLog log);
}
