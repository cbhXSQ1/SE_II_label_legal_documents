package com.seii.legal.function.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.QuestionStatus;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.DiffItem;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.AdjudicationDtos;
import com.seii.legal.rule.adjudication.AdjudicationRule;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjudicationAppServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private IdentityRule identityRule;
    @Mock private TaskRepository taskRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private AdjudicationRepository adjudicationRepository;
    @Mock private AdjudicationRule adjudicationRule;
    @Mock private DiagramRule diagramRule;
    @Mock private TaskRule taskRule;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AdjudicationAppService service;

    @BeforeEach
    void setUp() {
        AuthActionTemplate authTemplate = new AuthActionTemplate(sessionRepository, accountRepository, identityRule);
        service = new AdjudicationAppService(
                authTemplate,
                taskRepository,
                accountRepository,
                annotationRepository,
                adjudicationRepository,
                adjudicationRule,
                diagramRule,
                taskRule,
                new AnnotationPayloadAdapter(new ObjectMapper()),
                new AllowedActionResolver(),
                eventPublisher
        );
    }

    @Test
    void compareExecutesAuthScopeAndAdjudicationRule() {
        LoginUser arbiter = new LoginUser("u6", SystemType.ANNOTATION, IdentityType.ARBITER, "cred-u6", LocalDateTime.now().plusHours(2));
        Account arbiterAcc = new Account("u6", "wang", "123456", "王仲裁", "课程组", AccountStatus.ENABLED,
                List.of(new Role("ARBITER", "仲裁员", SystemType.ANNOTATION)));
        AnnotationTask task = new AnnotationTask("T1", SystemType.ANNOTATION, "劳务合同纠纷", "", LocalDateTime.now().plusDays(1),
                "v1.2", List.of("D1"), List.of("u3", "u4"), "u6", TaskStatus.CUTTING_PENDING, false, "u8", LocalDateTime.now(), null);
        Submission subA = new Submission("s1", "T1", "D1", RoundType.CUTTING, "u3", LocalDateTime.now(), "v1.2", "{\"pieces\":[]}", SubmissionStatus.SUBMITTED, null);
        Submission subB = new Submission("s2", "T1", "D1", RoundType.CUTTING, "u4", LocalDateTime.now(), "v1.2", "{\"pieces\":[]}", SubmissionStatus.SUBMITTED, null);

        when(sessionRepository.findByCredential("cred-u6")).thenReturn(Optional.of(arbiter));
        when(accountRepository.findById("u6")).thenReturn(Optional.of(arbiterAcc));
        when(accountRepository.findById("u3")).thenReturn(Optional.of(new Account("u3", "liu", "", "刘标注", "", AccountStatus.ENABLED, List.of())));
        when(accountRepository.findById("u4")).thenReturn(Optional.of(new Account("u4", "chen", "", "陈标注", "", AccountStatus.ENABLED, List.of())));
        when(identityRule.isEnabled(arbiterAcc)).thenReturn(true);
        when(identityRule.hasRole(arbiterAcc, IdentityType.ARBITER)).thenReturn(true);
        when(identityRule.resolveScope(arbiter, "T1", null)).thenReturn(DataScope.TASK_SUBMISSIONS);
        when(taskRepository.findById("T1")).thenReturn(Optional.of(task));
        when(annotationRepository.findSubmissions("T1", "D1", RoundType.CUTTING, null)).thenReturn(List.of(subA, subB));
        when(adjudicationRule.difference(subA.content(), subB.content(), RoundType.CUTTING))
                .thenReturn(new Agreement(97.0, 3.0, List.of(new DiffItem("d1", "标签选择", "P4", "SM", "SF", "类型不一致"))));

        AppResult<AdjudicationDtos.CompareResponse> res = service.compare(
                "cred-u6", new AdjudicationDtos.CompareRequest("T1", "D1", RoundType.CUTTING, "u3", "u4"));

        assertTrue(res.success());
        assertEquals(97.0, res.data().agreement());
        assertEquals("刘标注", res.data().versionA());
        assertEquals(1, res.data().items().size());
    }

    @Test
    void expertReplyUpdatesStatusAndPublishesDomainNotificationEvent() {
        LoginUser expert = new LoginUser("u7", SystemType.ANNOTATION, IdentityType.EXPERT, "cred-u7", LocalDateTime.now().plusHours(2));
        Account expertAcc = new Account("u7", "sun", "123456", "孙专家", "法学院", AccountStatus.ENABLED,
                List.of(new Role("EXPERT", "领域专家", SystemType.ANNOTATION)));
        ExpertQuestion q = new ExpertQuestion("q1", "T1", "D1", "c1", "4.2", "SM/SF", "A:SM B:SF", "u6", "u7",
                QuestionStatus.PENDING, "", LocalDateTime.now());

        when(sessionRepository.findByCredential("cred-u7")).thenReturn(Optional.of(expert));
        when(accountRepository.findById("u7")).thenReturn(Optional.of(expertAcc));
        when(identityRule.isEnabled(expertAcc)).thenReturn(true);
        when(identityRule.hasRole(expertAcc, IdentityType.EXPERT)).thenReturn(true);
        when(adjudicationRepository.findExpertQuestion("q1")).thenReturn(Optional.of(q));

        AppResult<Void> res = service.replyExpertQuestion("cred-u7",
                new AdjudicationDtos.ExpertReplyRequest("q1", "应标为 SM", "含道义规范标志", "指南4.2", false, false));

        assertTrue(res.success());
        verify(adjudicationRepository).updateQuestionStatus("q1", QuestionStatus.REPLIED);
        ArgumentCaptor<DomainNotificationEvent> captor = ArgumentCaptor.forClass(DomainNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(List.of("u6"), captor.getValue().receiverIds());
        assertEquals("EXPERT_REPLY", captor.getValue().type());
    }
}
