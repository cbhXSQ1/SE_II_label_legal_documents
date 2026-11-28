package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.rule.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternComponentsTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private NotificationService notificationService;

    @Test
    void taskQueryDispatcherRoutesByIdentity() {
        TaskQueryDispatcher dispatcher = new TaskQueryDispatcher(List.of(
                new TaskManagerQueryStrategy(taskRepository),
                new AnnotatorQueryStrategy(taskRepository),
                new ArbiterQueryStrategy(taskRepository),
                new AdminTaskQueryStrategy(taskRepository)
        ));
        AnnotationTask sample = new AnnotationTask("T1", SystemType.ANNOTATION, "示例任务", "", LocalDateTime.now(),
                "v1.2", List.of("D1"), List.of("u3", "u4"), "u6", TaskStatus.PUBLISHED, false, "u8", LocalDateTime.now(), null);
        when(taskRepository.findByAnnotator("u3", 1, 20)).thenReturn(new PageResult<>(List.of(sample), 1, 1, 20));
        when(taskRepository.findByArbiter("u6", 1, 20)).thenReturn(new PageResult<>(List.of(sample), 1, 1, 20));

        assertEquals(1, dispatcher.dispatch(IdentityType.ANNOTATOR, "u3", 1, 20).total());
        assertEquals(1, dispatcher.dispatch(IdentityType.ARBITER, "u6", 1, 20).total());
        verify(taskRepository).findByAnnotator("u3", 1, 20);
        verify(taskRepository).findByArbiter("u6", 1, 20);
    }

    @Test
    void allowedActionResolverReturnsExpectedActions() {
        AllowedActionResolver resolver = new AllowedActionResolver();
        assertTrue(resolver.resolve(TaskStatus.CONFIRMED, IdentityType.TASK_MANAGER).contains("导出数据"));
        assertTrue(resolver.resolve(TaskStatus.STRUCTURED_PENDING, IdentityType.ARBITER).contains("确认最终版本"));
        assertTrue(resolver.resolve(TaskStatus.WITHDRAWN, IdentityType.ANNOTATOR).isEmpty());
    }

    @Test
    void notificationListenerSwallowsMailFailureAndKeepsInApp() {
        when(notificationService.sendMail(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("SMTP unavailable"));
        NotificationEventListener listener = new NotificationEventListener(notificationService);

        assertDoesNotThrow(() -> listener.onDomainNotification(
                new DomainNotificationEvent(List.of("u3"), "APPROVAL", "审批通过", "内容", true)));
        verify(notificationService).sendInApp("u3", "内容", "APPROVAL");
    }
}
