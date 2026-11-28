package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.function.support.TaskQueryDispatcher;
import com.seii.legal.function.support.TaskQueryStrategy;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskQueryDispatcherTest {

    @Test
    void dispatchesToStrategyForIdentity() {
        RecordingStrategy annotator = new RecordingStrategy(IdentityType.ANNOTATOR,
                new PageResult<>(List.of(task("t1")), 1, 2, 10));
        TaskQueryDispatcher dispatcher = new TaskQueryDispatcher(List.of(
                new RecordingStrategy(IdentityType.TASK_MANAGER, empty(1, 20)),
                annotator,
                new RecordingStrategy(IdentityType.ARBITER, empty(1, 20))));

        PageResult<AnnotationTask> result = dispatcher.dispatch(
                IdentityType.ANNOTATOR, "u1", SystemType.ANNOTATION, 2, 10);

        assertTrue(annotator.called);
        assertEquals("u1", annotator.accountId);
        assertEquals(SystemType.ANNOTATION, annotator.system);
        assertEquals(2, annotator.page);
        assertEquals(10, annotator.size);
        assertEquals(1, result.records().size());
        assertEquals("t1", result.records().get(0).taskId());
    }

    @Test
    void dispatchPassesSystemToAdminStrategy() {
        RecordingStrategy admin = new RecordingStrategy(IdentityType.ADMIN,
                new PageResult<>(List.of(task("t9")), 1, 1, 20));
        TaskQueryDispatcher dispatcher = new TaskQueryDispatcher(List.of(admin));

        PageResult<AnnotationTask> result = dispatcher.dispatch(
                IdentityType.ADMIN, "root", SystemType.TEACHING, 1, 20);

        assertEquals(SystemType.TEACHING, admin.system);
        assertEquals("t9", result.records().get(0).taskId());
    }

    @Test
    void returnsEmptyPageWhenNoStrategyMatches() {
        RecordingStrategy annotator = new RecordingStrategy(IdentityType.ANNOTATOR, empty(1, 20));
        TaskQueryDispatcher dispatcher = new TaskQueryDispatcher(List.of(annotator));

        PageResult<AnnotationTask> result = dispatcher.dispatch(
                IdentityType.STUDENT, "u1", SystemType.ANNOTATION, 3, 5);

        assertFalse(annotator.called);
        assertTrue(result.records().isEmpty());
        assertEquals(0, result.total());
        assertEquals(3, result.page());
        assertEquals(5, result.size());
    }

    private static PageResult<AnnotationTask> empty(int page, int size) {
        return new PageResult<>(List.of(), 0, page, size);
    }

    private static AnnotationTask task(String id) {
        return new AnnotationTask(id, SystemType.ANNOTATION, "task", "desc", null, "v1",
                List.of("d1"), List.of("a1"), "arb", null, true, "mgr", null, null);
    }

    private static final class RecordingStrategy implements TaskQueryStrategy {
        private final IdentityType identity;
        private final PageResult<AnnotationTask> result;
        private boolean called;
        private String accountId;
        private SystemType system;
        private int page;
        private int size;

        private RecordingStrategy(IdentityType identity, PageResult<AnnotationTask> result) {
            this.identity = identity;
            this.result = result;
        }

        @Override
        public IdentityType supportedIdentity() {
            return identity;
        }

        @Override
        public PageResult<AnnotationTask> queryMyTasks(String accountId, int page, int size) {
            this.called = true;
            this.accountId = accountId;
            this.page = page;
            this.size = size;
            return result;
        }

        @Override
        public PageResult<AnnotationTask> queryMyTasks(String accountId, SystemType system, int page, int size) {
            this.system = system;
            return queryMyTasks(accountId, page, size);
        }
    }
}
