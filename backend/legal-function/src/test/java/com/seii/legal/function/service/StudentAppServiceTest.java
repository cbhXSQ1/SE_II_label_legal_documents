package com.seii.legal.function.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.ClassMember;
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.ImportRecord;
import com.seii.legal.data.entity.KnowledgePoint;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.LearningRecord;
import com.seii.legal.data.entity.LearningStat;
import com.seii.legal.data.entity.Question;
import com.seii.legal.data.entity.QuestionVersion;
import com.seii.legal.data.entity.Role;
import com.seii.legal.data.entity.StudentAccountResult;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskProgress;
import com.seii.legal.data.entity.TeacherFeedback;
import com.seii.legal.data.entity.TeachingClass;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.data.repository.TeachingRepository;
import com.seii.legal.function.dto.student.AnswerDetailView;
import com.seii.legal.function.dto.student.AnswerDraftCommand;
import com.seii.legal.function.dto.student.AnswerDraftItem;
import com.seii.legal.function.dto.student.AnswerDraftResult;
import com.seii.legal.function.dto.student.AnswerSubmitCommand;
import com.seii.legal.function.dto.student.LearningView;
import com.seii.legal.function.dto.student.MaterialView;
import com.seii.legal.function.dto.student.StudentAnswersView;
import com.seii.legal.function.dto.student.StudentExerciseDetailView;
import com.seii.legal.function.dto.student.StudentExercisesView;
import com.seii.legal.function.dto.student.StudentMaterialsView;
import com.seii.legal.function.dto.student.SubmitAnswerResult;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import com.seii.legal.rule.teaching.TeachingRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

class StudentAppServiceTest {

    private final LocalDateTime now = LocalDateTime.now();
    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeAccountRepository accounts = new FakeAccountRepository(
            new Account("u1", "student1", "pw", "张同学", "法学2501", AccountStatus.ENABLED,
                    List.of(new Role("STUDENT", "学生", SystemType.TEACHING))));
    private final FakeTeachingAdminRepository teachingAdmin = new FakeTeachingAdminRepository(
            new TeachingClass("c1", "C1", "法学2501", "t1", "2026", now));
    private final FakeTeachingRepository teaching = new FakeTeachingRepository();
    private final FakeIdentityRule identity = new FakeIdentityRule(Set.of(IdentityType.STUDENT), DataScope.OWN_CLASS);
    private final FakeTeachingRule teachingRule = new FakeTeachingRule();
    private final FakeTaskRule taskRule = new FakeTaskRule();
    private final StudentAppService service = new StudentAppService(
            sessions, accounts, teachingAdmin, teaching, identity, teachingRule, taskRule, new ObjectMapper());

    @BeforeEach
    void setUp() {
        sessions.saved.put("cred", new LoginUser("u1", SystemType.TEACHING, IdentityType.STUDENT,
                "cred", now.plusHours(1)));
    }

    @Test
    void materialsListPublishedClassMaterialsWithLearningProgress() {
        teaching.materials = List.of(material("m1", "c1"), material("m2", "c1"));
        teaching.learningRecords.put("m1", new LearningRecord("u1", "m1", now.minusHours(1), 120));

        AppResult<StudentMaterialsView> result = service.materials("cred");

        assertTrue(result.success());
        assertEquals(2, result.data().materials().size());
        assertEquals("c1", teaching.lastMaterialClassId);
        assertEquals("已发布", teaching.lastMaterialStatus);
        MaterialView first = result.data().materials().get(0);
        assertEquals("m1", first.id());
        assertTrue(first.studied());
        assertEquals(120, first.durationSeconds());
        assertEquals("已学习", first.completionStatus());
        assertFalse(result.data().materials().get(1).studied());
    }

    @Test
    void materialsRequireStudentRole() {
        identity.roles = Set.of(IdentityType.TEACHER);

        AppResult<StudentMaterialsView> result = service.materials("cred");

        assertFalse(result.success());
        assertEquals("只有学生可以访问", result.message());
    }

    @Test
    void exercisesShowCompletionAndRemainingTime() {
        teaching.exercises = List.of(exercise("e1", "c1", now.plusDays(1), "已发布"));
        teaching.answersByStudent.put("e1", List.of(answer("a1", "e1", "u1", 80, "已提交", now)));
        teachingRule.completionResult = "已提交";

        AppResult<StudentExercisesView> result = service.exercises("cred");

        assertTrue(result.success());
        assertEquals(1, result.data().exercises().size());
        var view = result.data().exercises().get(0);
        assertEquals("e1", view.id());
        assertEquals("已提交", view.completionStatus());
        assertTrue(view.remainingSeconds() > 0);
        assertFalse(view.overdue());
        assertTrue(view.allowRedo());
    }

    @Test
    void exerciseDetailRejectsExerciseFromAnotherClass() {
        teaching.exercise = exercise("e1", "c2", now.plusDays(1), "已发布");

        AppResult<StudentExerciseDetailView> result = service.exerciseDetail("cred", "e1");

        assertFalse(result.success());
        assertEquals("练习任务不存在", result.message());
    }

    @Test
    void exerciseDetailReturnsQuestionsInExerciseOrder() {
        teaching.exercise = exercise("e1", "c1", now.plusDays(1), "已发布");
        teaching.questionPool = List.of(question("q2"), question("q1"));

        AppResult<StudentExerciseDetailView> result = service.exerciseDetail("cred", "e1");

        assertTrue(result.success());
        assertEquals(List.of("q1", "q2"),
                result.data().questions().stream().map(Question::questionId).toList());
    }

    @Test
    void saveDraftStoresAggregateAnswers() {
        teaching.exercise = exercise("e1", "c1", now.plusDays(1), "已发布");

        AppResult<AnswerDraftResult> result = service.saveDraft("cred",
                new AnswerDraftCommand("e1", List.of(new AnswerDraftItem("q1", "甲"))));

        assertTrue(result.success());
        assertNotNull(result.data().savedAt());
        assertEquals("u1", teaching.savedDraft.studentId());
        assertEquals("e1", teaching.savedDraft.exerciseId());
        assertEquals("DRAFT", teaching.savedDraft.status());
        assertTrue(teaching.savedDraft.answer().contains("q1"));
        assertTrue(teaching.savedDraft.answer().contains("甲"));
    }

    @Test
    void submitScoresEachQuestionAveragesAndSavesDifferences() {
        teaching.exercise = exercise("e1", "c1", now.plusDays(1), "已发布");
        teaching.questionPool = List.of(question("q1"), question("q2"));
        teachingRule.scores.put("80", new TeachingRule.Score(80,
                List.of(new DifferenceItem("标签选择", "P1", "SF", "GF", "注意"))));
        teachingRule.scores.put("60", new TeachingRule.Score(60,
                List.of(new DifferenceItem("关系方向", "R1", "S", "A", "方向"))));
        teachingRule.averageResult = 70.0;
        teaching.saveAnswerId = "a1";

        AppResult<SubmitAnswerResult> result = service.submit("cred",
                new AnswerSubmitCommand("e1",
                        List.of(new AnswerDraftItem("q1", "80"), new AnswerDraftItem("q2", "60")),
                        "req-1"));

        assertTrue(result.success());
        assertEquals("a1", result.data().answerId());
        assertEquals(70.0, result.data().score());
        assertEquals("已提交", result.data().status());
        assertEquals(2, result.data().differences().size());
        assertTrue(result.data().systemFeedback().contains("差异"));
        assertEquals("req-1", teaching.lastRequestId);
        assertEquals(70.0, teaching.updatedScore);
        assertEquals(2, teaching.savedDifferences.size());
        assertEquals(List.of(80.0, 60.0), teachingRule.lastScores);
    }

    @Test
    void submitRejectsExpiredExercise() {
        teaching.exercise = exercise("e1", "c1", now.minusMinutes(1), "已发布");
        taskRule.canSubmitResult = false;

        AppResult<SubmitAnswerResult> result = service.submit("cred",
                new AnswerSubmitCommand("e1", List.of(), "req-1"));

        assertFalse(result.success());
        assertEquals("练习任务已截止", result.message());
        assertNull(teaching.savedAnswer);
    }

    @Test
    void submitRequiresRequestId() {
        AppResult<SubmitAnswerResult> result = service.submit("cred",
                new AnswerSubmitCommand("e1", List.of(), " "));

        assertFalse(result.success());
        assertEquals("请求标识不能为空", result.message());
    }

    @Test
    void answersAreListedNewestFirstForCurrentStudent() {
        identity.scope = DataScope.OWN_ANSWERS;
        AnswerRecord older = answer("a1", "e1", "u1", 70, "已提交", now.minusDays(1));
        AnswerRecord newer = answer("a2", "e1", "u1", 90, "已提交", now);
        teaching.answersByStudent.put("e1", List.of(older, newer));

        AppResult<StudentAnswersView> result = service.answers("cred", "e1");

        assertTrue(result.success());
        assertEquals(List.of("a2", "a1"),
                result.data().answers().stream().map(AnswerRecord::answerId).toList());
        assertEquals("u1", teaching.lastAnswersStudentId);
    }

    @Test
    void answerDetailRejectsAnotherStudentsAnswer() {
        identity.scope = DataScope.OWN_ANSWERS;
        teaching.answers.put("a2", answer("a2", "e1", "u2", 80, "已提交", now));

        AppResult<AnswerDetailView> result = service.answerDetail("cred", "a2");

        assertFalse(result.success());
        assertEquals("无权查看该作答记录", result.message());
    }

    @Test
    void answerDetailReturnsDifferencesAndTeacherFeedback() {
        identity.scope = DataScope.OWN_ANSWERS;
        teaching.answers.put("a1", answer("a1", "e1", "u1", 80, "已提交", now));
        teaching.differences.put("a1", List.of(new DifferenceItem("标签选择", "P1", "SF", "GF", "注意")));
        teaching.teacherFeedback.put("a1", new TeacherFeedback("a1", "t1", "继续努力", now));

        AppResult<AnswerDetailView> result = service.answerDetail("cred", "a1");

        assertTrue(result.success());
        assertEquals(1, result.data().differences().size());
        assertNotNull(result.data().teacherFeedback());
        assertEquals("继续努力", result.data().teacherFeedback().content());
        assertTrue(result.data().systemFeedback().contains("差异"));
    }

    @Test
    void answerDetailReportsTeacherFeedbackMissing() {
        identity.scope = DataScope.OWN_ANSWERS;
        teaching.answers.put("a1", answer("a1", "e1", "u1", 80, "已提交", now));
        teaching.differences.put("a1", List.of(new DifferenceItem("标签选择", "P1", "SF", "GF", "注意")));

        AppResult<AnswerDetailView> result = service.answerDetail("cred", "a1");

        assertTrue(result.success());
        assertEquals("教师还没有填写反馈", result.message());
        assertNull(result.data().teacherFeedback());
    }

    @Test
    void answerDetailReportsComparisonInProgress() {
        identity.scope = DataScope.OWN_ANSWERS;
        teaching.answers.put("a1", answer("a1", "e1", "u1", 0, "已提交", now));

        AppResult<AnswerDetailView> result = service.answerDetail("cred", "a1");

        assertFalse(result.success());
        assertEquals("正在比对，请稍后刷新", result.message());
    }

    @Test
    void learningReturnsMaterialsAnswersAndStats() {
        identity.scope = DataScope.OWN_ANSWERS;
        teaching.materials = List.of(material("m1", "c1"));
        teaching.learningRecords.put("m1", new LearningRecord("u1", "m1", now.minusHours(1), 60));
        teaching.answersByStudent.put("e1", List.of(answer("a1", "e1", "u1", 88, "已提交", now)));
        teaching.learningStat = new LearningStat(1, 88, 88, "已提交");

        AppResult<LearningView> result = service.learning("cred", "kp1", "e1", null, null);

        assertTrue(result.success());
        assertEquals(1, result.data().materials().size());
        assertEquals(1, result.data().answers().size());
        assertEquals(1, result.data().stats().size());
        assertEquals("c1", teaching.lastStatClassId);
        assertEquals("kp1", teaching.lastStatPointId);
        assertEquals("e1", teaching.lastStatExerciseId);
    }

    @Test
    void learningRequiresOwnAnswersScope() {
        identity.scope = DataScope.OWN_CLASS;

        AppResult<LearningView> result = service.learning("cred", null, null, null, null);

        assertFalse(result.success());
        assertEquals("当前身份不能查看该数据", result.message());
    }

    private LearningMaterial material(String id, String classId) {
        return new LearningMaterial(id, "材料" + id, "内容", List.of("kp1"), null, classId,
                "已发布", "t1", now.minusDays(1));
    }

    private Exercise exercise(String id, String classId, LocalDateTime deadline, String status) {
        return new Exercise(id, classId, "t1", "练习" + id, List.of("q1", "q2"), deadline, status, now.minusDays(1));
    }

    private Question question(String id) {
        return new Question(id, "c1", "t1", "单选", "题干", null, "中", List.of("kp1"), "{}",
                List.of("A", "B"), "A", "v1", 1, "已发布");
    }

    private AnswerRecord answer(String id, String exerciseId, String studentId, double score,
                                String status, LocalDateTime submittedAt) {
        return new AnswerRecord(id, exerciseId, studentId, null, "[]", submittedAt, score, status, null, null);
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, LoginUser> saved = new HashMap<>();

        @Override
        public String save(LoginUser user) {
            String credential = user.credential() == null || user.credential().isBlank()
                    ? "cred-" + (saved.size() + 1) : user.credential();
            saved.put(credential, new LoginUser(user.accountId(), user.system(), user.identity(),
                    credential, user.expiresAt()));
            return credential;
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
        private final Account account;

        private FakeAccountRepository(Account account) {
            this.account = account;
        }

        @Override
        public Optional<Account> findById(String accountId) {
            return account.accountId().equals(accountId) ? Optional.of(account) : Optional.empty();
        }

        @Override
        public Optional<Account> findByAccount(String login) {
            return account.account().equals(login) ? Optional.of(account) : Optional.empty();
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
            return account.roles();
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

    private static final class FakeIdentityRule implements IdentityRule {
        private Set<IdentityType> roles;
        private DataScope scope;

        private FakeIdentityRule(Set<IdentityType> roles, DataScope scope) {
            this.roles = roles;
            this.scope = scope;
        }

        @Override
        public boolean hasRole(Account account, IdentityType identity) {
            return roles.contains(identity);
        }

        @Override
        public DataScope resolveScope(LoginUser user, String taskId, String classId) {
            return scope;
        }

        @Override
        public boolean isEnabled(Account account) {
            return account.status() == AccountStatus.ENABLED;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            return List.of();
        }
    }

    private static final class FakeTeachingAdminRepository implements TeachingAdminRepository {
        private final TeachingClass teachingClass;

        private FakeTeachingAdminRepository(TeachingClass teachingClass) {
            this.teachingClass = teachingClass;
        }

        @Override
        public String saveClass(TeachingClass teachingClass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TeachingClass> findClass(String classId) {
            return teachingClass.classId().equals(classId) ? Optional.of(teachingClass) : Optional.empty();
        }

        @Override
        public Optional<TeachingClass> findClassByCode(String classCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<TeachingClass> findClasses(String term, String teacherId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveClassMember(String classId, String studentId, String studentNo,
                                    LocalDateTime joinedAt, String status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ClassMember> findClassMembers(String classId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void transferClass(String studentId, String fromClassId, String toClassId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveImportRecord(ImportRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ImportRecord> findImportRecord(String importId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TeachingClass> findClassByStudentId(String studentId) {
            return Optional.of(teachingClass);
        }

        @Override
        public Optional<Account> findByStudentNo(String studentNo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Account> findByTeacherNo(String teacherNo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StudentAccountResult createStudentAccount(String studentNo, String name, String classId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTeachingRepository implements TeachingRepository {
        private List<LearningMaterial> materials = List.of();
        private final Map<String, LearningRecord> learningRecords = new HashMap<>();
        private List<Exercise> exercises = List.of();
        private Exercise exercise;
        private List<Question> questionPool = List.of();
        private final Map<String, List<AnswerRecord>> answersByStudent = new HashMap<>();
        private final Map<String, AnswerRecord> answers = new HashMap<>();
        private final Map<String, List<DifferenceItem>> differences = new HashMap<>();
        private final Map<String, TeacherFeedback> teacherFeedback = new HashMap<>();
        private LearningStat learningStat;
        private String lastMaterialClassId;
        private String lastMaterialStatus;
        private AnswerRecord savedDraft;
        private AnswerRecord savedAnswer;
        private String saveAnswerId = "a1";
        private String lastRequestId;
        private double updatedScore;
        private List<DifferenceItem> savedDifferences = List.of();
        private String lastAnswersStudentId;
        private String lastStatClassId;
        private String lastStatPointId;
        private String lastStatExerciseId;

        @Override
        public String saveKnowledgePoint(KnowledgePoint point) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<KnowledgePoint> findKnowledgePoint(String pointId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteKnowledgePoint(String pointId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<KnowledgePoint> findKnowledgePoints(String teacherId, String category, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String saveMaterial(LearningMaterial material) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<LearningMaterial> findMaterial(String materialId) {
            return materials.stream().filter(item -> item.materialId().equals(materialId)).findFirst();
        }

        @Override
        public void updateMaterialStatus(String materialId, String status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<LearningMaterial> findMaterials(String classId, String status, int page, int size) {
            lastMaterialClassId = classId;
            lastMaterialStatus = status;
            return new PageResult<>(materials, materials.size(), page, size);
        }

        @Override
        public void saveLearningRecord(LearningRecord record) {
            learningRecords.put(record.materialId(), record);
        }

        @Override
        public Optional<LearningRecord> findLearningRecord(String studentId, String materialId) {
            return Optional.ofNullable(learningRecords.get(materialId));
        }

        @Override
        public String saveQuestion(Question question) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Question> findQuestion(String questionId) {
            return questionPool.stream().filter(item -> item.questionId().equals(questionId)).findFirst();
        }

        @Override
        public int saveQuestionVersion(String questionId, int version, String standardAnswer,
                                       String guideVersion, LocalDateTime effectiveAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<QuestionVersion> findQuestionVersion(String questionId, int version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Question> findQuestions(String classId, String pointId, int page, int size) {
            return new PageResult<>(questionPool, questionPool.size(), page, size);
        }

        @Override
        public String saveExercise(Exercise exercise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Exercise> findExercise(String exerciseId) {
            if (exercise != null && exercise.exerciseId().equals(exerciseId)) {
                return Optional.of(exercise);
            }
            return exercises.stream().filter(item -> item.exerciseId().equals(exerciseId)).findFirst();
        }

        @Override
        public void updateExerciseStatus(String exerciseId, String status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Exercise> findExercises(String classId, String status, int page, int size) {
            return new PageResult<>(exercises, exercises.size(), page, size);
        }

        @Override
        public String saveAnswer(AnswerRecord answer, String requestId) {
            savedAnswer = answer;
            lastRequestId = requestId;
            return saveAnswerId;
        }

        @Override
        public void saveAnswerDraft(AnswerRecord draft) {
            savedDraft = draft;
        }

        @Override
        public Optional<AnswerRecord> findAnswerDraft(String studentId, String exerciseId) {
            return Optional.ofNullable(savedDraft);
        }

        @Override
        public void deleteAnswerDraft(String studentId, String exerciseId) {
            savedDraft = null;
        }

        @Override
        public Optional<AnswerRecord> findAnswer(String answerId) {
            return Optional.ofNullable(answers.get(answerId));
        }

        @Override
        public List<AnswerRecord> findAnswers(String studentId, String exerciseId) {
            lastAnswersStudentId = studentId;
            List<AnswerRecord> records = new ArrayList<>(answersByStudent.getOrDefault(exerciseId, List.of()));
            records.sort(Comparator.comparing(AnswerRecord::submittedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            return records;
        }

        @Override
        public void updateAnswerStatus(String answerId, String status, String feedback,
                                       String teacherId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAnswerScore(String answerId, double score) {
            updatedScore = score;
        }

        @Override
        public void saveDifference(String answerId, List<DifferenceItem> items) {
            savedDifferences = new ArrayList<>(items);
        }

        @Override
        public List<DifferenceItem> findDifferences(String answerId, String questionId) {
            return differences.getOrDefault(answerId, List.of());
        }

        @Override
        public void saveTeacherFeedback(String answerId, String teacherId, String content, LocalDateTime time) {
            teacherFeedback.put(answerId, new TeacherFeedback(answerId, teacherId, content, time));
        }

        @Override
        public Optional<TeacherFeedback> findTeacherFeedback(String answerId) {
            return Optional.ofNullable(teacherFeedback.get(answerId));
        }

        @Override
        public LearningStat findLearningStat(String studentId, String classId, String pointId,
                                             String exerciseId, LocalDateTime from, LocalDateTime to) {
            lastStatClassId = classId;
            lastStatPointId = pointId;
            lastStatExerciseId = exerciseId;
            return learningStat;
        }

        @Override
        public void saveWeakThreshold(String classId, int threshold) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Integer> findWeakThreshold(String classId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTeachingRule implements TeachingRule {
        private final Map<String, Score> scores = new HashMap<>();
        private double averageResult;
        private String completionResult = "未开始";
        private List<Double> lastScores = List.of();

        @Override
        public boolean uniquePointName(String name, List<KnowledgePoint> points) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkMaterialPublish(LearningMaterial material, String guideVersion, String classId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkQuestion(String questionType, QuestionVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextVersion(Question question) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Score score(String answerContent, String standardAnswer, String questionType) {
            return scores.getOrDefault(answerContent, new Score(0, List.of()));
        }

        @Override
        public double average(List<Double> scores) {
            lastScores = new ArrayList<>(scores);
            return averageResult;
        }

        @Override
        public String completion(List<AnswerRecord> answers) {
            return completionResult;
        }

        @Override
        public KnowledgeStat summarize(List<AnswerRecord> answers, List<String> pointIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> weakPoints(KnowledgeStat stat, int threshold) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Question> targetedQuestions(List<String> pointIds, List<Question> pool) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long studyDuration(LocalDateTime first, LocalDateTime last, List<LocalDateTime> leaves) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeTaskRule implements TaskRule {
        private boolean canSubmitResult = true;

        @Override
        public String recognizeDocumentType(String selectedType, byte[] file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TextRange locateReasoning(String text, String documentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CaseInfo parseCaseInfo(String text, String documentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkCaseInfo(String documentType, CaseInfo info) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> checkPublish(AnnotationTask draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskStatus nextStatus(TaskStatus current, TaskAction action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskProgress progress(String taskId, List<Submission> submissions) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canSubmit(LocalDateTime deadline, LocalDateTime now) {
            return canSubmitResult;
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
}
