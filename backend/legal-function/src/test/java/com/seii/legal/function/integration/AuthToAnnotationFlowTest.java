package com.seii.legal.function.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.Draft;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.ApplicationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.LoginCommand;
import com.seii.legal.function.dto.SessionView;
import com.seii.legal.function.dto.annotation.LoadAnnotationView;
import com.seii.legal.function.service.AnnotationAppService;
import com.seii.legal.function.service.AuthAppService;
import com.seii.legal.function.support.AnnotationPayloadAdapter;
import com.seii.legal.function.support.AuthActionTemplate;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.annotation.ElementRule;
import com.seii.legal.rule.annotation.RelationRule;
import com.seii.legal.rule.annotation.SegmentationRule;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthToAnnotationFlowTest {

    @Test
    void loginCredentialLoadsAssignedTaskInAnnotationService() {
        Account account = new Account("u1", "alice", "pw", "Alice", "课程组",
                AccountStatus.ENABLED,
                List.of(new Role("ANNOTATOR", "标注员", SystemType.ANNOTATION)));
        InMemorySessionRepository sessions = new InMemorySessionRepository();
        AccountRepository accounts = mock(AccountRepository.class);
        ApplicationRepository applications = mock(ApplicationRepository.class);
        IdentityRule identities = mock(IdentityRule.class);

        when(accounts.findByAccount("alice")).thenReturn(Optional.of(account));
        when(accounts.findById("u1")).thenReturn(Optional.of(account));
        when(identities.isEnabled(account)).thenReturn(true);
        when(identities.hasRole(account, IdentityType.ANNOTATOR)).thenReturn(true);
        when(identities.availableIdentities(account)).thenReturn(List.of(
                new IdentityRule.IdentityOption(SystemType.ANNOTATION, IdentityType.ANNOTATOR, "标注")));
        when(identities.resolveScope(any(LoginUser.class), any(), any())).thenReturn(DataScope.OWN_TASKS);

        AuthAppService auth = new AuthAppService(sessions, accounts, applications, identities);
        AppResult<SessionView> login = auth.login(new LoginCommand("alice", "pw"));

        assertTrue(login.success());
        assertNotNull(login.data().credential());

        AnnotationTask task = new AnnotationTask("t1", SystemType.ANNOTATION, "任务", "",
                LocalDateTime.now().plusDays(1), "g1", List.of("d1"), List.of("u1"),
                "arbiter-1", TaskStatus.CUTTING_PENDING, true, "manager-1", null, null);
        TaskRound round = new TaskRound("r1", "t1", "d1", RoundType.CUTTING, 1, "OPEN",
                LocalDateTime.now(), null);
        TaskRepository tasks = mock(TaskRepository.class);
        when(tasks.findById("t1")).thenReturn(Optional.of(task));
        when(tasks.findCurrentRound(eq("t1"), eq("d1"), any())).thenReturn(Optional.of(round));

        AnnotationRepository annotations = mock(AnnotationRepository.class);
        when(annotations.findDraft("t1", "d1", "u1")).thenReturn(Optional.<Draft>empty());
        when(annotations.findSubmissionsByRound("r1", "u1")).thenReturn(List.<Submission>of());

        AuthActionTemplate template = new AuthActionTemplate(sessions, accounts, identities);
        AnnotationAppService annotation = new AnnotationAppService(
                template,
                tasks,
                annotations,
                mock(TaskRule.class),
                mock(SegmentationRule.class),
                mock(ElementRule.class),
                mock(RelationRule.class),
                mock(DiagramRule.class),
                new AnnotationPayloadAdapter(new ObjectMapper()));

        AppResult<LoadAnnotationView> loaded = annotation.load(login.data().credential(), "t1", "d1");

        assertTrue(loaded.success());
        assertFalse(loaded.data().submitted());
        assertEquals("CUTTING", loaded.data().round());
        assertTrue(loaded.data().content().pieces().isEmpty());
    }

    private static final class InMemorySessionRepository implements SessionRepository {
        private final Map<String, LoginUser> sessions = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            String credential = user.credential() == null || user.credential().isBlank()
                    ? "cred-" + (sessions.size() + 1) : user.credential();
            sessions.put(credential, new LoginUser(user.accountId(), user.system(), user.identity(),
                    credential, user.expiresAt()));
            return credential;
        }

        @Override
        public Optional<LoginUser> findByCredential(String credential) {
            return Optional.ofNullable(sessions.get(credential));
        }

        @Override
        public void delete(String credential) {
            sessions.remove(credential);
        }
    }
}
