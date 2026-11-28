package com.seii.legal.function.service;

import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.QuestionStatus;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.DiffItem;
import com.seii.legal.data.entity.ExpertQuestion;
import com.seii.legal.data.entity.ExpertReply;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.adjudication.CompareCommand;
import com.seii.legal.function.dto.adjudication.CompareResult;
import com.seii.legal.function.dto.adjudication.ConflictListView;
import com.seii.legal.function.dto.adjudication.ConflictQuery;
import com.seii.legal.function.dto.adjudication.DecideCommand;
import com.seii.legal.function.dto.adjudication.DiagramError;
import com.seii.legal.function.dto.adjudication.DiagramReviewCommand;
import com.seii.legal.function.dto.adjudication.ExpertQuestionCommand;
import com.seii.legal.function.dto.adjudication.ExpertQuestionCreated;
import com.seii.legal.function.dto.adjudication.ExpertQuestionView;
import com.seii.legal.function.dto.adjudication.ExpertReplyCommand;
import com.seii.legal.function.dto.adjudication.FinalConfirmCommand;
import com.seii.legal.function.dto.adjudication.FinalQuery;
import com.seii.legal.function.dto.adjudication.FinalVersionView;
import com.seii.legal.rule.adjudication.AdjudicationRule;
import com.seii.legal.rule.annotation.DiagramRule;
import com.seii.legal.rule.identity.IdentityRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdjudicationAppServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 10, 0);

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeTaskRepository tasks = new FakeTaskRepository();
    private final FakeAnnotationRepository annotations = new FakeAnnotationRepository();
    private final FakeAdjudicationRepository adjudications = new FakeAdjudicationRepository();
    private final FakeIdentityRule identities = new FakeIdentityRule();
    private final FakeAdjudicationRule rule = new FakeAdjudicationRule();
    private final FakeDiagramRule diagramRule = new FakeDiagramRule();
    private final AdjudicationAppService service = new AdjudicationAppService(
            sessions, accounts, tasks, annotations, adjudications, identities, rule, diagramRule);

    @Test
    void compareUsesCurrentRoundAndReportsDifferences() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);
        annotations.submissions = List.of(
                submission("s1", "t1", "d1", RoundType.STRUCTURED, "r1", "a1", "A"),
                submission("s2", "t1", "d1", RoundType.STRUCTURED, "r1", "a2", "B"));
        rule.agreement = new Agreement(97.0, 3.0, List.of(new DiffItem("标签选择", "P4", "SM", "SF", "判定")));

        AppResult<CompareResult> result = service.compare("cred",
                new CompareCommand("t1", "d1", RoundType.STRUCTURED, "a1", "a2"));

        assertTrue(result.success());
        assertEquals(97.0, result.data().agreement());
        assertEquals(3.0, result.data().difference());
        assertEquals("s1", result.data().versionA());
        assertEquals("s2", result.data().versionB());
        assertEquals("d1", result.data().items().get(0).id());
        assertEquals("标签选择", result.data().items().get(0).type());
        assertTrue(result.data().aboveEighty());
        assertEquals("r1", annotations.lastSubmissionRoundId);
        assertEquals(1, rule.differenceCalls);
        assertEquals(1, rule.aboveCalls);
    }

    @Test
    void compareRejectsNonArbiterOfTask() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-other", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);

        AppResult<CompareResult> result = service.compare("cred",
                new CompareCommand("t1", "d1", RoundType.STRUCTURED, "a1", "a2"));

        assertFalse(result.success());
        assertEquals("只有本任务仲裁员可以裁定", result.message());
        assertEquals(0, rule.differenceCalls);
    }

    @Test
    void compareRequiresCurrentRound() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");

        AppResult<CompareResult> result = service.compare("cred",
                new CompareCommand("t1", "d1", RoundType.STRUCTURED, "a1", "a2"));

        assertFalse(result.success());
        assertEquals("当前轮次不存在", result.message());
    }

    @Test
    void compareRequiresBothSubmissions() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);
        annotations.submissions = List.of(
                submission("s1", "t1", "d1", RoundType.STRUCTURED, "r1", "a1", "A"));

        AppResult<CompareResult> result = service.compare("cred",
                new CompareCommand("t1", "d1", RoundType.STRUCTURED, "a1", "a2"));

        assertFalse(result.success());
        assertEquals("乙的提交不存在", result.message());
    }

    @Test
    void conflictsCombinesDecisionsAndOutstandingDifferences() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);
        annotations.submissions = List.of(
                submission("s1", "t1", "d1", RoundType.STRUCTURED, "r1", "a1", "A"),
                submission("s2", "t1", "d1", RoundType.STRUCTURED, "r1", "a2", "B"));
        rule.classify = List.of(
                new DiffItem("标签选择", "P4", "SM", "SF", "x"),
                new DiffItem("标注边界", "P5", "1", "2", "y"));
        adjudications.decisionsByRound = List.of(decision("d1", "t1", "d1", "r1", "u-arb"));

        AppResult<ConflictListView> result = service.conflicts("cred",
                new ConflictQuery("t1", "d1", RoundType.STRUCTURED));

        assertTrue(result.success());
        assertEquals(2, result.data().conflicts().size());
        assertTrue(result.data().conflicts().get(0).decided());
        assertFalse(result.data().conflicts().get(1).decided());
        assertEquals(1, result.data().decidedCount());
        assertEquals(1, result.data().undecidedCount());
        assertEquals("r1", annotations.lastSubmissionRoundId);
    }

    @Test
    void decideSavesDecisionWithCurrentRound() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);

        AppResult<Void> result = service.decide("cred", new DecideCommand(
                "t1", "d1", RoundType.STRUCTURED, "d1", "标签选择", "P4",
                "SM", "SF", "认可甲", "SM", null));

        assertTrue(result.success());
        AdjudicationRecord saved = adjudications.lastDecision;
        assertNotNull(saved);
        assertEquals("r1", saved.roundId());
        assertEquals("u-arb", saved.arbiterId());
        assertEquals("SM", saved.result());
    }

    @Test
    void decideRejectsNonArbiter() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-other", TaskStatus.STRUCTURED_PENDING, "d1");

        AppResult<Void> result = service.decide("cred", new DecideCommand(
                "t1", "d1", RoundType.STRUCTURED, "d1", "标签选择", "P4",
                "SM", "SF", "认可甲", "SM", null));

        assertFalse(result.success());
        assertEquals("只有本任务仲裁员可以裁定", result.message());
        assertNull(adjudications.lastDecision);
    }

    @Test
    void askExpertSavesQuestion() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);

        AppResult<ExpertQuestionCreated> result = service.askExpert("cred",
                new ExpertQuestionCommand("t1", "d1", "指南3.2", "标签选择", "甲SM/乙SF", "u-exp", "疑难"));

        assertTrue(result.success());
        assertEquals("q-1", result.data().questionId());
        ExpertQuestion saved = adjudications.lastQuestion;
        assertNotNull(saved);
        assertEquals("t1", saved.taskId());
        assertEquals("d1", saved.documentId());
        assertEquals("u-exp", saved.expertId());
        assertEquals(QuestionStatus.PENDING, saved.status());
        assertTrue(saved.content().contains("指南3.2"));
    }

    @Test
    void expertQuestionsRequiresExpertAndFiltersBySelf() {
        login("u-exp", IdentityType.EXPERT);
        adjudications.expertQuestions = new PageResult<>(List.of(question("q1", "u-exp")), 1, 1, 20);

        AppResult<PageResult<ExpertQuestionView>> result = service.expertQuestions("cred", "PENDING", null, null);

        assertTrue(result.success());
        assertEquals(1, result.data().records().size());
        assertEquals("u-exp", adjudications.lastExpertQuestionExpertId);
        assertEquals(QuestionStatus.PENDING, adjudications.lastExpertQuestionStatus);
        assertEquals(1, adjudications.lastExpertQuestionPage);
        assertEquals(20, adjudications.lastExpertQuestionSize);
    }

    @Test
    void expertQuestionsRejectsArbiter() {
        login("u-arb", IdentityType.ARBITER);

        AppResult<PageResult<ExpertQuestionView>> result = service.expertQuestions("cred", null, 1, 20);

        assertFalse(result.success());
        assertEquals("只有领域专家可以回复问题", result.message());
    }

    @Test
    void replyExpertSavesReplyAndUpdatesStatus() {
        login("u-exp", IdentityType.EXPERT);
        adjudications.questions.put("q1", question("q1", "u-exp"));

        AppResult<Void> result = service.replyExpert("cred",
                new ExpertReplyCommand("q1", "应为SM", "依据指南", true, false));

        assertTrue(result.success());
        ExpertReply reply = adjudications.lastReply;
        assertNotNull(reply);
        assertEquals("q1", reply.questionId());
        assertEquals("u-exp", reply.expertId());
        assertTrue(reply.content().contains("应为SM"));
        assertEquals(QuestionStatus.NEED_MORE, adjudications.lastQuestionStatus);
    }

    @Test
    void replyExpertRejectsOtherExpert() {
        login("u-exp", IdentityType.EXPERT);
        adjudications.questions.put("q1", question("q1", "u-other"));

        AppResult<Void> result = service.replyExpert("cred",
                new ExpertReplyCommand("q1", "应为SM", "依据指南", false, false));

        assertFalse(result.success());
        assertEquals("只有被指定的专家可以回复", result.message());
        assertNull(adjudications.lastReply);
    }

    @Test
    void reviewDiagramSavesReview() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        annotations.diagrams.put("dg1", new Diagram("dg1", "rel1", List.of(), List.of(), "{}", 1, null, "u", NOW));

        AppResult<Void> result = service.reviewDiagram("cred",
                new DiagramReviewCommand("t1", "d1", "dg1", true, List.of()));

        assertTrue(result.success());
        DiagramReview review = adjudications.lastReview;
        assertNotNull(review);
        assertEquals("通过", review.result());
        assertEquals("u-arb", review.reviewerId());
        assertEquals(1, diagramRule.checkCalls);
    }

    @Test
    void reviewDiagramRejectsPassedWhenConsistencyFails() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        annotations.diagrams.put("dg1", new Diagram("dg1", "rel1", List.of(), List.of(), "{}", 1, null, "u", NOW));
        diagramRule.consistency = List.of("符号错误");

        AppResult<Void> result = service.reviewDiagram("cred",
                new DiagramReviewCommand("t1", "d1", "dg1", true, List.of(new DiagramError("n1", "符号错误", "应为实心圆"))));

        assertFalse(result.success());
        assertTrue(result.message().contains("符号错误"));
        assertNull(adjudications.lastReview);
    }

    @Test
    void finalVersionReturnsCurrentRoundFinalVersion() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.CONFIRMED, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);
        adjudications.finalVersion = Optional.of(new FinalVersion(
                "fv1", "t1", "d1", "r1", VersionType.FINAL, "c1", "e1", "rel1", "dg1",
                "v1", "u-arb", NOW, "CONFIRMED"));

        AppResult<FinalVersionView> result = service.finalVersion("cred", new FinalQuery("t1", "d1"));

        assertTrue(result.success());
        assertEquals("c1", result.data().content().cutVersionId());
        assertEquals("dg1", result.data().content().diagramId());
        assertEquals("v1", result.data().guideVersion());
        assertEquals("CONFIRMED", result.data().status());
    }

    @Test
    void finalVersionMissingFails() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);

        AppResult<FinalVersionView> result = service.finalVersion("cred", new FinalQuery("t1", "d1"));

        assertFalse(result.success());
        assertEquals("最终版本不存在", result.message());
    }

    @Test
    void finalConfirmSavesFinalVersionAndConfirmsTask() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);
        annotations.unifiedCut = Optional.of(new CutVersion("c1", "t1", "d1", "u-arb",
                RoundType.STRUCTURED, "r1", List.of(), List.of(), NOW));
        annotations.latestElementVersion = Optional.of("e1");
        annotations.latestRelationVersion = Optional.of("rel1");
        annotations.diagramByRelationVersion = Optional.of(
                new Diagram("dg1", "rel1", List.of(), List.of(), "{}", 1, null, "u", NOW));
        adjudications.decisionsByRound = List.of(decision("d1", "t1", "d1", "r1", "u-arb"));
        adjudications.reviews = List.of(new DiagramReview("rv1", "t1", "d1", "dg1",
                "u-arb", NOW, "通过", List.of()));

        AppResult<Void> result = service.finalConfirm("cred", new FinalConfirmCommand("t1", "d1"));

        assertTrue(result.success());
        FinalVersion saved = adjudications.lastFinalVersion;
        assertNotNull(saved);
        assertEquals(VersionType.FINAL, saved.versionType());
        assertEquals("r1", saved.roundId());
        assertEquals("c1", saved.cutVersionId());
        assertEquals("e1", saved.elementVersionId());
        assertEquals("rel1", saved.relationVersionId());
        assertEquals("dg1", saved.diagramId());
        assertEquals("u-arb", saved.confirmedBy());
        assertEquals("t1", adjudications.confirmedTaskId);
        assertEquals("u-arb", adjudications.confirmedBy);
    }

    @Test
    void finalConfirmRejectsWhenIncomplete() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-arb", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);
        rule.finalIssues = List.of("切分冲突未裁定", "图示未通过");

        AppResult<Void> result = service.finalConfirm("cred", new FinalConfirmCommand("t1", "d1"));

        assertFalse(result.success());
        assertTrue(result.message().contains("切分冲突未裁定"));
        assertNull(adjudications.lastFinalVersion);
        assertNull(adjudications.confirmedTaskId);
    }

    @Test
    void finalConfirmRejectsNonArbiter() {
        login("u-arb", IdentityType.ARBITER);
        task("t1", "u-other", TaskStatus.STRUCTURED_PENDING, "d1");
        round("r1", "t1", "d1", RoundType.STRUCTURED);

        AppResult<Void> result = service.finalConfirm("cred", new FinalConfirmCommand("t1", "d1"));

        assertFalse(result.success());
        assertEquals("只有本任务仲裁员可以裁定", result.message());
    }

    @Test
    void expiredCredentialFails() {
        AppResult<CompareResult> result = service.compare("missing",
                new CompareCommand("t1", "d1", RoundType.STRUCTURED, "a1", "a2"));

        assertFalse(result.success());
        assertEquals("登录已失效，请重新登录", result.message());
    }

    private void login(String accountId, IdentityType identity) {
        LoginUser user = new LoginUser(accountId, SystemType.ANNOTATION, identity,
                "cred", NOW.plusHours(1));
        sessions.users.put("cred", user);
        accounts.accounts.put(accountId, new Account(accountId, accountId, "pw", accountId, "课程组",
                AccountStatus.ENABLED, List.of(new Role(identity.name(), identity.name(), SystemType.ANNOTATION))));
        identities.roles = Set.of(identity);
    }

    private void task(String taskId, String arbiterId, TaskStatus status, String documentId) {
        tasks.tasks.put(taskId, new AnnotationTask(taskId, SystemType.ANNOTATION, "任务", "说明",
                NOW.plusDays(1), "v1", List.of(documentId), List.of("a1", "a2"), arbiterId,
                status, true, "u-mgr", NOW.minusDays(1), null));
    }

    private void round(String roundId, String taskId, String documentId, RoundType type) {
        tasks.rounds.put(taskId + "|" + documentId + "|" + type,
                new TaskRound(roundId, taskId, documentId, type, 1, "OPEN", NOW, null));
    }

    private static Submission submission(String id, String taskId, String documentId, RoundType round,
                                         String roundId, String annotatorId, String content) {
        return new Submission(id, taskId, documentId, round, roundId, annotatorId, NOW, "v1", content,
                com.seii.legal.common.enums.SubmissionStatus.SUBMITTED, null);
    }

    private static AdjudicationRecord decision(String conflictId, String taskId, String documentId,
                                               String roundId, String arbiterId) {
        return new AdjudicationRecord("dec-" + conflictId, taskId, documentId, RoundType.STRUCTURED,
                roundId, conflictId, "标签选择", "P4", "SM", "SF", "认可甲", "SM", arbiterId, NOW,
                null, null, null, null);
    }

    private static ExpertQuestion question(String id, String expertId) {
        return new ExpertQuestion(id, "t1", "d1", "d1", expertId, "问题", QuestionStatus.PENDING, NOW, null);
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, LoginUser> users = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            users.put(user.credential(), user);
            return user.credential();
        }

        @Override
        public Optional<LoginUser> findByCredential(String credential) {
            return Optional.ofNullable(users.get(credential));
        }

        @Override
        public void delete(String credential) {
            users.remove(credential);
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new HashMap<>();

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(accounts.get(accountId));
        }

        @Override
        public Optional<Account> findByAccount(String account) {
            throw new UnsupportedOperationException();
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
            return accounts.get(accountId).roles();
        }

        @Override
        public void saveRoles(String accountId, List<Role> roles, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveChangeLog(com.seii.legal.data.entity.AccountChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeIdentityRule implements IdentityRule {
        private Set<IdentityType> roles = Set.of();

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return roles.contains(identity);
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return DataScope.TASK_SUBMISSIONS;
        }

        @Override
        public boolean isEnabled(Account account) {
            return account.status() == AccountStatus.ENABLED;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTaskRepository implements TaskRepository {
        private final Map<String, AnnotationTask> tasks = new HashMap<>();
        private final Map<String, TaskRound> rounds = new HashMap<>();

        @Override
        public Optional<AnnotationTask> findById(String taskId) {
            return Optional.ofNullable(tasks.get(taskId));
        }

        @Override
        public Optional<TaskRound> findCurrentRound(String taskId, String documentId, RoundType type) {
            return Optional.ofNullable(rounds.get(taskId + "|" + documentId + "|" + type));
        }

        @Override
        public String save(AnnotationTask task) {
            throw new UnsupportedOperationException();
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
        public Optional<com.seii.legal.data.entity.TaskAssignment> findAssignment(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveRound(TaskRound round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TaskRound> findRound(String roundId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TaskRound> findRounds(String taskId, String documentId) {
            throw new UnsupportedOperationException();
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
        public com.seii.legal.data.entity.TaskProgress findProgress(String taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveChangeLog(com.seii.legal.data.entity.TaskChangeLog log) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeAnnotationRepository implements AnnotationRepository {
        private List<Submission> submissions = List.of();
        private Optional<CutVersion> unifiedCut = Optional.empty();
        private Optional<String> latestElementVersion = Optional.empty();
        private Optional<String> latestRelationVersion = Optional.empty();
        private Optional<Diagram> diagramByRelationVersion = Optional.empty();
        private final Map<String, Diagram> diagrams = new HashMap<>();
        private final Map<String, List<Relation>> relationsByVersion = new HashMap<>();
        private String lastSubmissionRoundId;

        @Override
        public List<Submission> findSubmissionsByRound(String roundId, String annotatorId) {
            lastSubmissionRoundId = roundId;
            return submissions;
        }

        @Override
        public List<Submission> findSubmissions(String taskId, String documentId, RoundType round, String annotatorId) {
            throw new AssertionError("must use findSubmissionsByRound");
        }

        @Override
        public Optional<CutVersion> findCutVersionByRound(String roundId, String annotatorId) {
            return unifiedCut;
        }

        @Override
        public Optional<Diagram> findDiagram(String diagramId) {
            return Optional.ofNullable(diagrams.get(diagramId));
        }

        @Override
        public List<Relation> findRelationsByVersion(String relationVersionId) {
            return relationsByVersion.getOrDefault(relationVersionId, List.of());
        }

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
        public String saveElements(String cutVersionId, List<com.seii.legal.data.entity.Element> elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.seii.legal.data.entity.Element> findElements(String cutVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.seii.legal.data.entity.Element> findElementsByVersion(String elementVersionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findLatestElementVersion(String cutVersionId) {
            return latestElementVersion;
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
        public Optional<String> findLatestRelationVersion(String elementVersionId) {
            return latestRelationVersion;
        }

        @Override
        public String saveDiagram(Diagram diagram) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Diagram> findDiagramByRelationVersion(String relationVersionId) {
            return diagramByRelationVersion;
        }

        @Override
        public void saveDraft(com.seii.legal.data.entity.Draft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<com.seii.legal.data.entity.Draft> findDraft(String taskId, String documentId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<com.seii.legal.data.entity.Draft> findDraftByRound(String roundId, String annotatorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveSubmission(Submission submission, String requestId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateSubmissionStatus(String submissionId, com.seii.legal.common.enums.SubmissionStatus status,
                                           String returnReason, String auditNote) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeAdjudicationRepository implements AdjudicationRepository {
        private List<AdjudicationRecord> decisionsByRound = List.of();
        private final Map<String, ExpertQuestion> questions = new HashMap<>();
        private PageResult<ExpertQuestion> expertQuestions = new PageResult<>(List.of(), 0, 1, 20);
        private List<DiagramReview> reviews = List.of();
        private Optional<FinalVersion> finalVersion = Optional.empty();
        private AdjudicationRecord lastDecision;
        private ExpertQuestion lastQuestion;
        private ExpertReply lastReply;
        private DiagramReview lastReview;
        private FinalVersion lastFinalVersion;
        private QuestionStatus lastQuestionStatus;
        private String confirmedTaskId;
        private String confirmedBy;
        private String lastExpertQuestionExpertId;
        private QuestionStatus lastExpertQuestionStatus;
        private int lastExpertQuestionPage;
        private int lastExpertQuestionSize;

        @Override
        public String saveDecision(AdjudicationRecord record) {
            lastDecision = record;
            return "dec-1";
        }

        @Override
        public List<AdjudicationRecord> findDecisions(String taskId, String documentId, RoundType round) {
            throw new AssertionError("must use findDecisionsByRound");
        }

        @Override
        public List<AdjudicationRecord> findDecisionsByRound(String roundId) {
            return decisionsByRound;
        }

        @Override
        public String saveExpertQuestion(ExpertQuestion question) {
            lastQuestion = question;
            questions.put("q-1", question);
            return "q-1";
        }

        @Override
        public Optional<ExpertQuestion> findExpertQuestion(String questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }

        @Override
        public PageResult<ExpertQuestion> findExpertQuestions(String expertId, QuestionStatus status, int page, int size) {
            lastExpertQuestionExpertId = expertId;
            lastExpertQuestionStatus = status;
            lastExpertQuestionPage = page;
            lastExpertQuestionSize = size;
            return expertQuestions;
        }

        @Override
        public String saveExpertReply(ExpertReply reply) {
            lastReply = reply;
            return "rep-1";
        }

        @Override
        public void updateQuestionStatus(String questionId, QuestionStatus status) {
            lastQuestionStatus = status;
        }

        @Override
        public List<DiagramReview> findDiagramReviews(String taskId, String documentId) {
            return reviews;
        }

        @Override
        public void saveDiagramReview(DiagramReview review) {
            lastReview = review;
        }

        @Override
        public String saveFinalVersion(FinalVersion version) {
            lastFinalVersion = version;
            return "fv-1";
        }

        @Override
        public Optional<FinalVersion> findFinalVersion(String taskId, String documentId) {
            return finalVersion;
        }

        @Override
        public Optional<FinalVersion> findFinalVersion(String taskId, String documentId, VersionType versionType) {
            return finalVersion;
        }

        @Override
        public Optional<FinalVersion> findFinalVersionByRound(String roundId, VersionType versionType) {
            return finalVersion;
        }

        @Override
        public void confirmTask(String taskId, String confirmerId, LocalDateTime time) {
            confirmedTaskId = taskId;
            confirmedBy = confirmerId;
        }
    }

    private static final class FakeAdjudicationRule implements AdjudicationRule {
        private Agreement agreement = new Agreement(100.0, 0.0, List.of());
        private List<DiffItem> classify = List.of();
        private boolean aboveEighty = true;
        private List<String> finalIssues = List.of();
        private int differenceCalls;
        private int aboveCalls;

        @Override
        public Agreement difference(String a, String b, RoundType round) {
            differenceCalls++;
            return agreement;
        }

        @Override
        public List<DiffItem> classify(String a, String b, RoundType round) {
            return classify;
        }

        @Override
        public boolean aboveEighty(Agreement agreement) {
            aboveCalls++;
            return aboveEighty;
        }

        @Override
        public List<String> fourPrinciples(List<DiffItem> cutConflicts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkFinalVersion(List<AdjudicationRecord> decisions, List<DiagramReview> reviews,
                                              CutVersion unifiedCut) {
            return finalIssues;
        }

        @Override
        public boolean singleAnnotator(int submissionCount) {
            return submissionCount < 2;
        }
    }

    private static final class FakeDiagramRule implements DiagramRule {
        private List<String> consistency = List.of();
        private int checkCalls;

        @Override
        public List<String> checkConsistency(Diagram diagram, List<Relation> relations) {
            checkCalls++;
            return consistency;
        }

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
        public Agreement compareDiagrams(Diagram a, Diagram b) {
            throw new UnsupportedOperationException();
        }
    }
}
