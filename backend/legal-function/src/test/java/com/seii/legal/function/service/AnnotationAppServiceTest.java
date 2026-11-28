package com.seii.legal.function.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.Draft;
import com.seii.legal.data.entity.Element;
import com.seii.legal.data.entity.Proposition;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskAssignment;
import com.seii.legal.data.entity.TaskChangeLog;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.DataConflictException;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.annotation.LoadAnnotationView;
import com.seii.legal.function.dto.annotation.SaveDraftCommand;
import com.seii.legal.function.dto.annotation.SaveDraftView;
import com.seii.legal.function.dto.annotation.SubmitCommand;
import com.seii.legal.function.dto.annotation.SubmitView;
import com.seii.legal.function.support.AnnotationPayloadAdapter;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.annotation.ElementRule;
import com.seii.legal.rule.annotation.RelationRule;
import com.seii.legal.rule.annotation.SegmentationRule;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationAppServiceTest {

    private static final String TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeTaskRepository tasks = new FakeTaskRepository();
    private final FakeAnnotationRepository annotations = new FakeAnnotationRepository();
    private final FakeIdentityRule identities = new FakeIdentityRule();
    private final FakeTaskRule taskRuleFake = new FakeTaskRule();
    private final FakeSegmentationRule segmentationRuleFake = new FakeSegmentationRule();
    private final FakeElementRule elementRuleFake = new FakeElementRule();
    private final FakeRelationRule relationRuleFake = new FakeRelationRule();
    private final FakeDiagramRule diagramRuleFake = new FakeDiagramRule();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AnnotationPayloadAdapter adapter = new AnnotationPayloadAdapter(mapper);
    private final AnnotationAppService service = new AnnotationAppService(
            sessions, accounts, tasks, annotations, identities, taskRuleFake,
            segmentationRuleFake, elementRuleFake, relationRuleFake, diagramRuleFake, adapter);

    @BeforeEach
    void setUp() {
        sessions.saved.put("cred-1", new LoginUser("u1", SystemType.ANNOTATION,
                IdentityType.ANNOTATOR, "cred-1", LocalDateTime.now().plusHours(1)));
        accounts.account = new Account("u1", "alice", "pw", "Alice", "课程组",
                AccountStatus.ENABLED, List.of(new Role("ANNOTATOR", "标注员", SystemType.ANNOTATION)));
        tasks.tasks.put("t1", task(TaskStatus.PUBLISHED, List.of("u1")));
        tasks.rounds.add(new TaskRound("r-cut", "t1", "d1", RoundType.CUTTING, 1,
                "OPEN", LocalDateTime.now(), null));
    }

    @Test
    void loadReturnsDraftContentAndCurrentCuttingRound() {
        annotations.drafts.add(new Draft("t1", "d1", "u1", "r-cut",
                adapter.listToJson(pieces()), adapter.listToJson(segments()),
                adapter.listToJson(relationPayload()), adapter.listToJson(diagramPayload()),
                LocalDateTime.now()));

        AppResult<LoadAnnotationView> result = service.load("cred-1", "t1", "d1");

        assertTrue(result.success());
        assertFalse(result.data().submitted());
        assertEquals("CUTTING", result.data().round());
        assertEquals("s1", result.data().content().pieces().get(0).get("id"));
        assertEquals("P1", result.data().content().segments().get(0).get("id"));
        assertEquals(1, result.data().content().relations().size());
        assertEquals(1, result.data().content().diagram().size());
        assertEquals("r-cut", annotations.lastDraftRoundId);
    }

    @Test
    void loadSubmissionFromCurrentRoundIsReadOnlyAndAuthoritative() {
        String content = adapter.toStorageJson(new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "submitted-piece")), segments(), List.of(), List.of()), null);
        annotations.submissions.add(new Submission("s0", "t1", "d1", RoundType.CUTTING, "r-cut",
                "u1", LocalDateTime.now(), "v1", content, SubmissionStatus.SUBMITTED, null));
        annotations.drafts.add(new Draft("t1", "d1", "u1", "r-cut",
                adapter.listToJson(pieces()), adapter.listToJson(segments()),
                adapter.listToJson(relationPayload()), adapter.listToJson(diagramPayload()),
                LocalDateTime.now()));

        AppResult<LoadAnnotationView> result = service.load("cred-1", "t1", "d1");

        assertTrue(result.success());
        assertTrue(result.data().submitted());
        assertEquals("submitted-piece", result.data().content().pieces().get(0).get("id"));
        assertEquals("CUTTING", result.data().round());
        assertEquals("r-cut", annotations.lastSubmissionRoundId);
    }

    @Test
    void loadRejectsMissingSession() {
        AppResult<LoadAnnotationView> result = service.load("bad", "t1", "d1");

        assertFalse(result.success());
        assertEquals("登录已失效，请重新登录", result.message());
    }

    @Test
    void loadRejectsDisabledAccount() {
        identities.enabled = false;

        AppResult<LoadAnnotationView> result = service.load("cred-1", "t1", "d1");

        assertFalse(result.success());
        assertEquals("账号已停用，请联系管理员", result.message());
    }

    @Test
    void loadRejectsNonAnnotatorIdentity() {
        sessions.saved.put("cred-1", new LoginUser("u1", SystemType.ANNOTATION,
                IdentityType.ARBITER, "cred-1", LocalDateTime.now().plusHours(1)));

        AppResult<LoadAnnotationView> result = service.load("cred-1", "t1", "d1");

        assertFalse(result.success());
        assertEquals("只有本任务标注员可以标注", result.message());
    }

    @Test
    void loadRejectsTaskNotAssignedToAccount() {
        tasks.tasks.put("t1", task(TaskStatus.PUBLISHED, List.of("u2")));

        AppResult<LoadAnnotationView> result = service.load("cred-1", "t1", "d1");

        assertFalse(result.success());
        assertEquals("只有本任务标注员可以标注", result.message());
    }

    @Test
    void saveDraftUsesCurrentRoundAndNestedContentWins() {
        AnnotationPayloadAdapter.AnnotationContent nested = new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "nested")), segments(), relationPayload(), diagramPayload());

        AppResult<SaveDraftView> result = service.saveDraft("cred-1",
                new SaveDraftCommand("t1", "d1", nested, pieces(), segments(),
                        relationPayload(), diagramPayload()));

        assertTrue(result.success());
        assertTrue(result.data().savedAt().matches(TIME_PATTERN));
        Draft draft = annotations.savedDraft;
        assertNotNull(draft);
        assertEquals("t1", draft.taskId());
        assertEquals("d1", draft.documentId());
        assertEquals("u1", draft.annotatorId());
        assertEquals("r-cut", draft.roundId());
        assertTrue(draft.cutJson().contains("nested"));
        assertFalse(draft.cutJson().contains("flat"));
        assertTrue(draft.elementsJson().contains("P1"));
        assertTrue(draft.relationsJson().contains("R1"));
        assertTrue(draft.diagramJson().contains("dg1"));
    }

    @Test
    void saveDraftAcceptsFlatPayload() {
        AppResult<SaveDraftView> result = service.saveDraft("cred-1",
                new SaveDraftCommand("t1", "d1", null, pieces(), segments(),
                        relationPayload(), diagramPayload()));

        assertTrue(result.success());
        assertTrue(annotations.savedDraft.cutJson().contains("s1"));
        assertTrue(annotations.savedDraft.elementsJson().contains("P1"));
    }

    @Test
    void saveDraftRejectsSubmittedRound() {
        annotations.submissions.add(new Submission("s0", "t1", "d1", RoundType.CUTTING, "r-cut",
                "u1", LocalDateTime.now(), "v1", "{}", SubmissionStatus.SUBMITTED, null));

        AppResult<SaveDraftView> result = service.saveDraft("cred-1",
                new SaveDraftCommand("t1", "d1", null, pieces(), segments(),
                        relationPayload(), diagramPayload()));

        assertFalse(result.success());
        assertEquals("本轮已经提交", result.message());
        assertNull(annotations.savedDraft);
    }

    @Test
    void saveDraftRejectsMissingCurrentRound() {
        tasks.rounds.clear();

        AppResult<SaveDraftView> result = service.saveDraft("cred-1",
                new SaveDraftCommand("t1", "d1", null, pieces(), segments(),
                        relationPayload(), diagramPayload()));

        assertFalse(result.success());
        assertEquals("当前轮次不存在", result.message());
    }

    @Test
    void submitCuttingRoundSavesSubmissionWithRequestIdAndProgress() {
        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertTrue(result.success());
        assertEquals("s1", result.data().submissionId());
        assertTrue(result.data().submittedAt().matches(TIME_PATTERN));
        assertEquals("req-1", annotations.savedRequestId);
        Submission saved = annotations.savedSubmission;
        assertNotNull(saved);
        assertNull(saved.submissionId());
        assertEquals("t1", saved.taskId());
        assertEquals("d1", saved.documentId());
        assertEquals(RoundType.CUTTING, saved.round());
        assertEquals("r-cut", saved.roundId());
        assertEquals("u1", saved.annotatorId());
        assertEquals("v1", saved.guideVersion());
        assertEquals(SubmissionStatus.SUBMITTED, saved.status());
        assertEquals(2, adapter.fromStorageJson(saved.content()).segments().size());
        assertTrue(taskRuleFake.progressCalled);
        assertEquals("t1", taskRuleFake.progressTaskId);
        assertEquals(1, taskRuleFake.progressSubmissions.size());
    }

    @Test
    void submitRejectsAfterDeadline() {
        taskRuleFake.canSubmit = false;

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("已过截止时间，不能提交", result.message());
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitRejectsInvalidTaskStatusTransition() {
        taskRuleFake.nextStatus = null;

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("当前任务状态不能提交", result.message());
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitCuttingRoundRejectsUncutPositions() {
        segmentationRuleFake.uncut = List.of(3);

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("未完成项：存在未切分位置", result.message());
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitCuttingRoundRejectsEmptySegments() {
        AppResult<SubmitView> result = service.submit("cred-1",
                new SubmitCommand("t1", "d1", null, null, pieces(), List.of(),
                        relationPayload(), diagramPayload(), "req-1"));

        assertFalse(result.success());
        assertEquals("未完成项：尚未切分", result.message());
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitStructuredRoundRejectsUnassignedElements() {
        useStructuredRound();
        elementRuleFake.unassigned = List.of("P2");

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("未完成项：存在未分配要素：P2", result.message());
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitStructuredRoundRejectsIsolatedPropositions() {
        useStructuredRound();
        relationRuleFake.isolated = List.of("P3");

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("未完成项：存在孤立命题：P3", result.message());
        assertEquals(List.of("P1", "P2"), relationRuleFake.lastPropositionIds);
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitStructuredRoundRejectsInconsistentDiagram() {
        useStructuredRound();
        diagramRuleFake.errors = List.of("缺少边");

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("未完成项：图示不一致：缺少边", result.message());
        assertNotNull(diagramRuleFake.lastDiagram);
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitStructuredRoundSucceedsWhenComplete() {
        useStructuredRound();
        taskRuleFake.nextStatus = TaskStatus.STRUCTURED_PENDING;

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertTrue(result.success());
        assertEquals(RoundType.STRUCTURED, annotations.savedSubmission.round());
        assertEquals("r-str", annotations.savedSubmission.roundId());
    }

    @Test
    void submitRejectsBlankRequestId() {
        AppResult<SubmitView> result = service.submit("cred-1", submitCommand(" "));

        assertFalse(result.success());
        assertEquals("缺少请求标识", result.message());
        assertNull(annotations.savedSubmission);
    }

    @Test
    void submitRejectsMissingCurrentRound() {
        tasks.rounds.clear();

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("当前轮次不存在", result.message());
    }

    @Test
    void submitMapsDataConflictToFailure() {
        annotations.saveSubmissionError = new DataConflictException("本轮已经提交");

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("本轮已经提交", result.message());
    }

    @Test
    void submitRejectsNonAnnotator() {
        sessions.saved.put("cred-1", new LoginUser("u1", SystemType.ANNOTATION,
                IdentityType.ARBITER, "cred-1", LocalDateTime.now().plusHours(1)));

        AppResult<SubmitView> result = service.submit("cred-1", submitCommand("req-1"));

        assertFalse(result.success());
        assertEquals("只有本任务标注员可以标注", result.message());
        assertNull(annotations.savedSubmission);
    }

    private void useStructuredRound() {
        tasks.tasks.put("t1", task(TaskStatus.CUTTING_CONFIRMED, List.of("u1")));
        tasks.rounds.clear();
        tasks.rounds.add(new TaskRound("r-str", "t1", "d1", RoundType.STRUCTURED, 1,
                "OPEN", LocalDateTime.now(), null));
    }

    private SubmitCommand submitCommand(String requestId) {
        return new SubmitCommand("t1", "d1", null, null, pieces(), segments(),
                relationPayload(), diagramPayload(), requestId);
    }

    private AnnotationTask task(TaskStatus status, List<String> annotatorIds) {
        return new AnnotationTask("t1", SystemType.ANNOTATION, "任务", "说明",
                LocalDateTime.now().plusDays(1), "v1", List.of("d1"), annotatorIds, "arb",
                status, true, "mgr", LocalDateTime.now(), null);
    }

    private static List<Map<String, Object>> pieces() {
        return List.of(Map.of("id", "s1", "text", "依法成立的合同，自成立时生效。"));
    }

    private static List<Map<String, Object>> segments() {
        return List.of(
                Map.of("id", "P1", "text", "依法成立的合同，", "type", "SM"),
                Map.of("id", "P2", "text", "自成立时生效。", "type", "GF"));
    }

    private static List<Map<String, Object>> relationPayload() {
        return List.of(Map.of("id", "R1", "type", "S", "members", List.of("P1", "P2"),
                "formal", "S(P1, P2)"));
    }

    private static List<Map<String, Object>> diagramPayload() {
        return List.of(Map.of(
                "id", "dg1",
                "nodes", List.of(Map.of("id", "P1", "x", 10, "y", 20)),
                "relNodes", List.of(Map.of("id", "n1", "kind", "support", "x", 30, "y", 40)),
                "edges", List.of(Map.of("from", "P1", "to", "n1", "arrow", false))));
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, LoginUser> saved = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            saved.put(user.credential(), user);
            return user.credential();
        }

        @Override
        public Optional<LoginUser> findByCredential(String credential) {
            return Optional.ofNullable(saved.get(credential));
        }

        @Override
        public void delete(String credential) {
            saved.remove(credential);
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        private Account account;

        @Override
        public Optional<Account> findById(String accountId) {
            return account != null && account.accountId().equals(accountId)
                    ? Optional.of(account) : Optional.empty();
        }

        @Override
        public Optional<Account> findByAccount(String login) {
            return account != null && account.account().equals(login)
                    ? Optional.of(account) : Optional.empty();
        }

        @Override
        public PageResult<Account> search(String keyword, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Account> search(String keyword, IdentityType identity, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String save(Account account) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(String accountId, AccountStatus status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findRoles(String accountId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveRoles(String accountId, List<Role> roles, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveChangeLog(AccountChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTaskRepository implements TaskRepository {
        private final Map<String, AnnotationTask> tasks = new HashMap<>();
        private final List<TaskRound> rounds = new ArrayList<>();

        @Override
        public String save(AnnotationTask task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<AnnotationTask> findById(String taskId) {
            return Optional.ofNullable(tasks.get(taskId));
        }

        @Override
        public void update(String taskId, AnnotationTask changes, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStatus(String taskId, TaskStatus status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAssignment(String taskId, List<String> annotatorIds, String arbiterId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TaskAssignment> findAssignment(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveRound(TaskRound round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TaskRound> findRound(String roundId) {
            return rounds.stream().filter(r -> r.roundId().equals(roundId)).findFirst();
        }

        @Override
        public Optional<TaskRound> findCurrentRound(String taskId, String documentId, RoundType type) {
            return rounds.stream()
                    .filter(r -> r.taskId().equals(taskId)
                            && r.documentId().equals(documentId)
                            && r.type() == type
                            && r.closedAt() == null)
                    .findFirst();
        }

        @Override
        public List<TaskRound> findRounds(String taskId, String documentId) {
            return rounds.stream()
                    .filter(r -> r.taskId().equals(taskId) && r.documentId().equals(documentId))
                    .toList();
        }

        @Override
        public void closeRound(String roundId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findByManager(String managerId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findByArbiter(String arbiterId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findByAnnotator(String annotatorId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<AnnotationTask> findAll(SystemType system, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskProgress findProgress(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveChangeLog(TaskChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeAnnotationRepository implements AnnotationRepository {
        private final List<Draft> drafts = new ArrayList<>();
        private final List<Submission> submissions = new ArrayList<>();
        private Draft savedDraft;
        private Submission savedSubmission;
        private String savedRequestId;
        private RuntimeException saveSubmissionError;
        private String lastDraftRoundId;
        private String lastSubmissionRoundId;

        @Override
        public String saveCutVersion(CutVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CutVersion> findCutVersion(String versionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CutVersion> findCutVersion(String taskId, String documentId, String annotatorId, RoundType round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CutVersion> findCutVersionByRound(String roundId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveElements(String cutVersionId, List<Element> elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Element> findElements(String cutVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Element> findElementsByVersion(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findLatestElementVersion(String cutVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveRelations(String elementVersionId, List<Relation> relations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Relation> findRelations(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Relation> findRelationsByVersion(String relationVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findLatestRelationVersion(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveDiagram(Diagram diagram) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Diagram> findDiagram(String diagramId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Diagram> findDiagramByRelationVersion(String relationVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveDraft(Draft draft) {
            savedDraft = draft;
            drafts.add(draft);
        }

        @Override
        public Optional<Draft> findDraft(String taskId, String documentId, String annotatorId) {
            Optional<Draft> found = drafts.stream()
                    .filter(d -> d.taskId().equals(taskId)
                            && d.documentId().equals(documentId)
                            && d.annotatorId().equals(annotatorId))
                    .reduce((first, second) -> second);
            found.ifPresent(d -> lastDraftRoundId = d.roundId());
            return found;
        }

        @Override
        public Optional<Draft> findDraftByRound(String roundId, String annotatorId) {
            return drafts.stream()
                    .filter(d -> d.roundId().equals(roundId) && d.annotatorId().equals(annotatorId))
                    .reduce((first, second) -> second);
        }

        @Override
        public String saveSubmission(Submission submission, String requestId) {
            if (saveSubmissionError != null) {
                throw saveSubmissionError;
            }
            savedSubmission = submission;
            savedRequestId = requestId;
            String id = "s1";
            submissions.add(new Submission(id, submission.taskId(), submission.documentId(),
                    submission.round(), submission.roundId(), submission.annotatorId(),
                    submission.submittedAt(), submission.guideVersion(), submission.content(),
                    submission.status(), submission.returnReason()));
            return id;
        }

        @Override
        public List<Submission> findSubmissions(String taskId, String documentId, RoundType round, String annotatorId) {
            return submissions.stream()
                    .filter(s -> s.taskId().equals(taskId)
                            && s.documentId().equals(documentId)
                            && s.round() == round
                            && (annotatorId == null || s.annotatorId().equals(annotatorId)))
                    .toList();
        }

        @Override
        public List<Submission> findSubmissionsByRound(String roundId, String annotatorId) {
            lastSubmissionRoundId = roundId;
            return submissions.stream()
                    .filter(s -> s.roundId().equals(roundId)
                            && (annotatorId == null || s.annotatorId().equals(annotatorId)))
                    .toList();
        }

        @Override
        public void updateSubmissionStatus(String submissionId, SubmissionStatus status,
                                           String returnReason, String auditNote) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeIdentityRule implements IdentityRule {
        private boolean enabled = true;
        private final Set<IdentityType> roles = new HashSet<>(Set.of(IdentityType.ANNOTATOR));

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return roles.contains(identity);
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return DataScope.OWN_TASKS;
        }

        @Override
        public boolean isEnabled(Account account) {
            return enabled;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            return List.of();
        }
    }

    private static final class FakeTaskRule implements TaskRule {
        private boolean canSubmit = true;
        private TaskStatus nextStatus = TaskStatus.CUTTING_PENDING;
        private boolean progressCalled;
        private String progressTaskId;
        private List<Submission> progressSubmissions;

        @Override
        public String recognizeDocumentType(String selectedType, byte[] file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TextRange locateReasoning(String text, String documentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.seii.legal.data.entity.CaseInfo parseCaseInfo(String text, String documentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkCaseInfo(String documentType, com.seii.legal.data.entity.CaseInfo info) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkPublish(AnnotationTask draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskStatus nextStatus(TaskStatus current, TaskAction action) {
            return nextStatus;
        }

        @Override
        public TaskProgress progress(String taskId, List<Submission> submissions) {
            progressCalled = true;
            progressTaskId = taskId;
            progressSubmissions = submissions;
            return new TaskProgress(1, 1, 0, 0, 0, 0);
        }

        @Override
        public boolean canSubmit(LocalDateTime deadline, LocalDateTime now) {
            return canSubmit;
        }

        @Override
        public boolean hasEnoughAnnotators(List<String> annotatorIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasSingleArbiter(String arbiterId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeSegmentationRule implements SegmentationRule {
        private List<Integer> uncut = List.of();
        private List<Integer> lastCutPoints;
        private String lastText;

        @Override
        public List<Integer> cutPoints(String text, String guideVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> numberParagraphs(List<Integer> cutPoints, String text) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> renumber(List<Integer> cutPoints, CutOperation operation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> findUncut(List<Integer> cutPoints, String text) {
            lastCutPoints = cutPoints;
            lastText = text;
            return uncut;
        }

        @Override
        public Agreement compareCut(List<Integer> cutA, List<Integer> cutB) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeElementRule implements ElementRule {
        private List<String> unassigned = List.of();

        @Override
        public String classifyNonProposition(String paragraph, String guideVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String classifyProposition(String paragraph, String guideVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String classifyPropositionSubType(String paragraph, String propositionType, String guideVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPropositionCandidate(Element element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> findUnassigned(List<Element> elements) {
            return unassigned;
        }

        @Override
        public Agreement compareElements(List<Element> a, List<Element> b) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeRelationRule implements RelationRule {
        private List<String> isolated = List.of();
        private List<String> lastPropositionIds;

        @Override
        public String classify(Proposition a, Proposition b, String guideVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String nextStep(List<Relation> existing) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> sortComposite(List<String> propositionIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String matchDirection(Proposition individual, Proposition general) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String attackDirection(Proposition attacker, Proposition target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSame(Proposition a, Proposition b) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValidNesting(String expression) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> findIsolated(List<Relation> relations, List<String> propositionIds) {
            lastPropositionIds = propositionIds;
            return isolated;
        }

        @Override
        public Agreement compareRelations(List<Relation> a, List<Relation> b) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeDiagramRule implements DiagramRule {
        private List<String> errors = List.of();
        private Diagram lastDiagram;

        @Override
        public Diagram generate(List<Relation> relations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String symbol(String relationType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DiagramLayout layout(List<Relation> relations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkConsistency(Diagram diagram, List<Relation> relations) {
            lastDiagram = diagram;
            return errors;
        }

        @Override
        public Agreement compareDiagrams(Diagram a, Diagram b) {
            throw new UnsupportedOperationException();
        }
    }
}
