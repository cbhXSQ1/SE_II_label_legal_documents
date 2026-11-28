package com.seii.legal.function.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.enums.AccountStatus;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AccountChangeLog;
import com.seii.legal.data.entity.AnswerRecord;
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
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.entity.StudentAccountResult;
import com.seii.legal.data.entity.TeacherFeedback;
import com.seii.legal.data.entity.TeachingClass;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.data.repository.TeachingRepository;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.ReadingRecord;
import com.seii.legal.function.dto.teacher.ClassLearningView;
import com.seii.legal.function.dto.teacher.ExerciseCommand;
import com.seii.legal.function.dto.teacher.FeedbackCommand;
import com.seii.legal.function.dto.teacher.IdResult;
import com.seii.legal.function.dto.teacher.KnowledgePointCommand;
import com.seii.legal.function.dto.teacher.MaterialCommand;
import com.seii.legal.function.dto.teacher.MaterialStatusCommand;
import com.seii.legal.function.dto.teacher.PointDeleteCommand;
import com.seii.legal.function.dto.teacher.QuestionCommand;
import com.seii.legal.function.dto.teacher.QuestionResult;
import com.seii.legal.function.dto.teacher.TargetedView;
import com.seii.legal.function.dto.teacher.ThresholdCommand;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.notification.NotificationService;
import com.seii.legal.rule.teaching.TeachingRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherAppServiceTest {

    private static final String CRED = "cred-teacher";
    private static final String TEACHER_ID = "u2";

    private final FakeSessionRepository sessions = new FakeSessionRepository();
    private final FakeIdentityRule identities = new FakeIdentityRule();
    private final FakeAccountRepository accounts = new FakeAccountRepository();
    private final FakeTeachingRepository teaching = new FakeTeachingRepository();
    private final FakeTeachingAdminRepository teachingAdmin = new FakeTeachingAdminRepository();
    private final FakeGuideRepository guides = new FakeGuideRepository();
    private final FakeFileRepository files = new FakeFileRepository();
    private final FakeTeachingRule rule = new FakeTeachingRule();
    private final FakeNotificationService notifications = new FakeNotificationService();
    private final TeacherAppService service = new TeacherAppService(
            sessions, accounts, identities, teaching, guides, files, teachingAdmin, rule,
            notifications, new ObjectMapper());

    TeacherAppServiceTest() {
        sessions.saved.put(CRED, new LoginUser(TEACHER_ID, SystemType.TEACHING, IdentityType.TEACHER,
                CRED, LocalDateTime.now().plusHours(1)));
        accounts.put(account(TEACHER_ID, "赵老师"));
        accounts.put(account("s1", "张三"));
        accounts.put(account("s2", "李四"));
        teachingAdmin.classes.put("c1", new TeachingClass("c1", "LAW2501", "法学2501", TEACHER_ID,
                "2026秋", LocalDateTime.now()));
        teachingAdmin.classes.put("c9", new TeachingClass("c9", "LAW2599", "别人的班", "u9",
                "2026秋", LocalDateTime.now()));
    }

    @Test
    void pointsRejectExpiredCredential() {
        AppResult<PageResult<KnowledgePoint>> result = service.points("bad", null, 1, 20);

        assertFalse(result.success());
        assertEquals("登录已失效，请重新登录", result.message());
    }

    @Test
    void pointsRejectDisabledAccount() {
        identities.enabled = false;

        AppResult<PageResult<KnowledgePoint>> result = service.points(CRED, null, 1, 20);

        assertFalse(result.success());
        assertEquals("账号已停用，请联系管理员", result.message());
    }

    @Test
    void pointsRejectNonTeacherIdentity() {
        sessions.saved.put(CRED, new LoginUser("s1", SystemType.TEACHING, IdentityType.STUDENT,
                CRED, LocalDateTime.now().plusHours(1)));
        accounts.put(account("s1", "张三"));

        AppResult<PageResult<KnowledgePoint>> result = service.points(CRED, null, 1, 20);

        assertFalse(result.success());
        assertEquals("只有教师可以访问教学功能", result.message());
    }

    @Test
    void pointsRejectForeignDataScope() {
        identities.scope = DataScope.OWN_ANSWERS;

        AppResult<PageResult<KnowledgePoint>> result = service.points(CRED, null, 1, 20);

        assertFalse(result.success());
        assertEquals("当前身份不能访问这个班级", result.message());
    }

    @Test
    void pointsListScopesToTeacherAndNormalizesPaging() {
        teaching.points.put("kp1", point("kp1", "命题要素", "命题要素", TEACHER_ID));
        teaching.points.put("kp2", point("kp2", "法律关系", "命题要素", TEACHER_ID));
        teaching.points.put("kp9", point("kp9", "别人的点", "命题要素", "u9"));

        AppResult<PageResult<KnowledgePoint>> result = service.points(CRED, "命题要素", 0, 0);

        assertTrue(result.success());
        assertEquals(2, result.data().total());
        assertEquals(1, result.data().page());
        assertEquals(20, result.data().size());
        assertTrue(result.data().records().stream().allMatch(p -> TEACHER_ID.equals(p.teacherId())));
    }

    @Test
    void savePointRejectsDuplicateName() {
        rule.uniqueName = false;

        AppResult<IdResult> result = service.savePoint(CRED,
                new KnowledgePointCommand(null, "命题要素", "命题要素", "说明", "中"));

        assertFalse(result.success());
        assertEquals("知识点名称已经存在", result.message());
        assertTrue(teaching.savedPoints.isEmpty());
    }

    @Test
    void savePointCreatesWithTeacherOwnership() {
        AppResult<IdResult> result = service.savePoint(CRED,
                new KnowledgePointCommand(null, "命题要素", "命题要素", "说明", "中"));

        assertTrue(result.success());
        KnowledgePoint saved = teaching.points.get(result.data().id());
        assertEquals(TEACHER_ID, saved.teacherId());
        assertEquals("命题要素", saved.name());
    }

    @Test
    void updatePointRejectsForeignPoint() {
        teaching.points.put("kp9", point("kp9", "别人的点", "命题要素", "u9"));

        AppResult<IdResult> result = service.savePoint(CRED,
                new KnowledgePointCommand("kp9", "改名", "命题要素", "说明", "中"));

        assertFalse(result.success());
        assertEquals("只能修改自己的知识点", result.message());
    }

    @Test
    void deletePointRejectsForeignPoint() {
        teaching.points.put("kp9", point("kp9", "别人的点", "命题要素", "u9"));

        AppResult<Void> result = service.deletePoint(CRED, new PointDeleteCommand("kp9"));

        assertFalse(result.success());
        assertEquals("只能修改自己的知识点", result.message());
        assertTrue(teaching.points.containsKey("kp9"));
    }

    @Test
    void saveMaterialRejectsMissingPublishFields() {
        rule.materialMissing = List.of("内容", "指南版本", "发布班级");

        AppResult<IdResult> result = service.saveMaterial(CRED,
                new MaterialCommand(null, "指南讲解", "讲义", "", null, "", List.of("kp1"), "", true));

        assertFalse(result.success());
        assertEquals("缺少发布条件：内容、指南版本、发布班级", result.message());
        assertTrue(teaching.savedMaterials.isEmpty());
    }

    @Test
    void saveMaterialPublishesAndRegistersGuideReference() {
        AppResult<IdResult> result = service.saveMaterial(CRED,
                new MaterialCommand(null, "指南讲解", "讲义", "内容", null, "v1.2",
                        List.of("kp1"), "c1", true));

        assertTrue(result.success());
        LearningMaterial saved = teaching.materials.get(result.data().id());
        assertEquals(TEACHER_ID, saved.createdBy());
        assertEquals("c1", saved.classId());
        assertEquals("MATERIAL", guides.savedReferences.get(0).get(0));
        assertEquals(saved.materialId(), guides.savedReferences.get(0).get(1));
        assertEquals("v1.2", guides.savedReferences.get(0).get(2));
    }

    @Test
    void saveMaterialRejectsForeignClass() {
        AppResult<IdResult> result = service.saveMaterial(CRED,
                new MaterialCommand(null, "指南讲解", "讲义", "内容", null, "v1.2",
                        List.of("kp1"), "c9", false));

        assertFalse(result.success());
        assertEquals("只能操作自己班级的学习材料", result.message());
    }

    @Test
    void materialStatusRejectsForeignMaterial() {
        teaching.materials.put("m9", new LearningMaterial("m9", "别处材料", "内容", List.of(),
                null, "c9", "已发布", "u9", LocalDateTime.now()));

        AppResult<Void> result = service.updateMaterialStatus(CRED,
                new MaterialStatusCommand("m9", "已下架"));

        assertFalse(result.success());
        assertEquals("只能修改自己的学习材料", result.message());
        assertEquals("已发布", teaching.materials.get("m9").status());
    }

    @Test
    void saveQuestionCreatesVersionAndKeepsStandard() {
        Map<String, Object> standard = Map.of("cut", "P1|P2");

        AppResult<QuestionResult> result = service.saveQuestion(CRED,
                new QuestionCommand(null, "切割", "题干", null, "中", List.of("kp1"),
                        List.of(), "答案", standard, "v1.2", "c1"));

        assertTrue(result.success());
        assertEquals(1, result.data().version());
        Question saved = teaching.questions.get(result.data().id());
        assertEquals(TEACHER_ID, saved.teacherId());
        assertEquals("{\"cut\":\"P1|P2\"}", saved.standardJson());
        assertEquals(1, teaching.savedVersions.size());
        assertEquals(result.data().version(), teaching.savedVersions.get(0).version());
    }

    @Test
    void saveQuestionRejectsMissingStandard() {
        rule.questionMissing = List.of("标准结果");

        AppResult<QuestionResult> result = service.saveQuestion(CRED,
                new QuestionCommand(null, "切割", "题干", null, "中", List.of("kp1"),
                        List.of(), null, null, "v1.2", "c1"));

        assertFalse(result.success());
        assertEquals("题目缺少：标准结果", result.message());
        assertTrue(teaching.savedQuestions.isEmpty());
    }

    @Test
    void saveQuestionUpdateBumpsVersionAndKeepsOld() {
        teaching.questions.put("q1", question("q1", 2));
        rule.nextVersion = 3;

        AppResult<QuestionResult> result = service.saveQuestion(CRED,
                new QuestionCommand("q1", "要素标注", "新题干", null, "中", List.of("kp1"),
                        List.of(), "答案", Map.of("k", "v"), "v1.3", "c1"));

        assertTrue(result.success());
        assertEquals(3, result.data().version());
        assertEquals(3, teaching.questions.get("q1").version());
        assertEquals(3, teaching.savedVersions.get(0).version());
    }

    @Test
    void saveQuestionRejectsForeignQuestion() {
        teaching.questions.put("q9", new Question("q9", "c9", "u9", "切割", "题干", null, "中",
                List.of(), "{}", List.of(), "答案", "v1.2", 1, "PUBLISHED"));

        AppResult<QuestionResult> result = service.saveQuestion(CRED,
                new QuestionCommand("q9", "切割", "题干", null, "中", List.of("kp1"),
                        List.of(), "答案", Map.of(), "v1.2", "c1"));

        assertFalse(result.success());
        assertEquals("只能修改自己的题目", result.message());
    }

    @Test
    void saveExerciseRejectsEmptyQuestionList() {
        AppResult<IdResult> result = service.saveExercise(CRED,
                new ExerciseCommand(null, "练习一", "说明", LocalDateTime.now().plusDays(2), true,
                        "v1.2", "c1", List.of()));

        assertFalse(result.success());
        assertEquals("请至少选择一道题目", result.message());
        assertTrue(teaching.savedExercises.isEmpty());
    }

    @Test
    void saveExerciseStoresOwnershipAndGuideReference() {
        AppResult<IdResult> result = service.saveExercise(CRED,
                new ExerciseCommand(null, "练习一", "说明", LocalDateTime.now().plusDays(2), true,
                        "v1.2", "c1", List.of("q1")));

        assertTrue(result.success());
        Exercise saved = teaching.exercises.get(result.data().id());
        assertEquals(TEACHER_ID, saved.teacherId());
        assertEquals("c1", saved.classId());
        assertEquals("EXERCISE", guides.savedReferences.get(0).get(0));
    }

    @Test
    void classLearningUsesDefaultThresholdAndComputesWeakPoints() {
        teachingAdmin.members.add(new ClassMember("c1", "s1", "2025001", LocalDateTime.now(), "ACTIVE"));
        teaching.stats.put("s1", new LearningStat(3, 55.0, 62.0, "已提交"));
        rule.weakPoints = List.of("kp1");

        AppResult<ClassLearningView> result = service.classLearning(CRED, "c1", null, "kp1", null);

        assertTrue(result.success());
        assertEquals(60, result.data().threshold());
        assertEquals(1, result.data().students().size());
        assertEquals("张三", result.data().students().get(0).name());
        assertEquals(3, result.data().students().get(0).practiceCount());
        assertEquals(55.0, result.data().students().get(0).bestScore());
        assertEquals(List.of("kp1"), result.data().students().get(0).weakPoints());
        assertEquals(60, rule.lastWeakThreshold);
    }

    @Test
    void classLearningRejectsForeignClass() {
        AppResult<ClassLearningView> result = service.classLearning(CRED, "c9", null, "kp1", null);

        assertFalse(result.success());
        assertEquals("只能查看自己班级的学习情况", result.message());
    }

    @Test
    void thresholdSavesForOwnClassAndDefaultsToSixty() {
        AppResult<Void> result = service.saveThreshold(CRED, new ThresholdCommand("c1", null));

        assertTrue(result.success());
        assertEquals(60, teaching.thresholds.get("c1"));
    }

    @Test
    void targetedUsesThresholdAndWeakPointRule() {
        teaching.thresholds.put("c1", 75);
        teaching.stats.put("kp1", new LearningStat(2, 45.0, 50.0, "已提交"));
        teaching.stats.put("kp2", new LearningStat(1, 80.0, 80.0, "已提交"));
        teaching.questions.put("q1", question("q1", 1));
        rule.weakPoints = List.of("kp1");
        rule.targeted = List.of(teaching.questions.get("q1"));

        AppResult<TargetedView> result = service.targeted(CRED, "c1", "kp1,kp2");

        assertTrue(result.success());
        assertEquals(1, result.data().points().size());
        assertEquals("kp1", result.data().points().get(0).pointId());
        assertEquals(45.0, result.data().points().get(0).bestScore());
        assertEquals(1, result.data().questions().size());
        assertEquals(75, rule.lastWeakThreshold);
    }

    @Test
    void feedbackRequiresScoreWhenMissing() {
        teaching.answers.put("a1", answer("a1", 0.0, "待教师批改"));
        teaching.exercises.put("e1", exercise("e1"));

        AppResult<Void> result = service.feedback(CRED, new FeedbackCommand("a1", null, "写得好"));

        assertFalse(result.success());
        assertEquals("请先录入得分", result.message());
    }

    @Test
    void feedbackRequiresContent() {
        teaching.answers.put("a1", answer("a1", 0.0, "待教师批改"));
        teaching.exercises.put("e1", exercise("e1"));

        AppResult<Void> result = service.feedback(CRED, new FeedbackCommand("a1", 88.0, " "));

        assertFalse(result.success());
        assertEquals("请填写反馈内容", result.message());
    }

    @Test
    void feedbackRejectsForeignClassAnswer() {
        teaching.answers.put("a1", answer("a1", 0.0, "待教师批改"));
        teaching.exercises.put("e1", new Exercise("e1", "c9", "u9", "别处练习", List.of("q1"),
                LocalDateTime.now().plusDays(1), "PUBLISHED", LocalDateTime.now()));

        AppResult<Void> result = service.feedback(CRED, new FeedbackCommand("a1", 88.0, "写得好"));

        assertFalse(result.success());
        assertEquals("只能评价自己班级的作答", result.message());
    }

    @Test
    void feedbackSavesScoreStatusAndNotifiesStudent() {
        teaching.answers.put("a1", answer("a1", 0.0, "待教师批改"));
        teaching.exercises.put("e1", exercise("e1"));

        AppResult<Void> result = service.feedback(CRED, new FeedbackCommand("a1", 88.0, "写得好"));

        assertTrue(result.success());
        assertEquals(88.0, teaching.scores.get("a1"));
        assertEquals("已批改", teaching.statuses.get("a1"));
        assertEquals("写得好", teaching.feedbacks.get("a1").content());
        assertEquals("s1", notifications.sent.get(0).receiverId());
        assertEquals("TEACHER_FEEDBACK", notifications.sent.get(0).type());
    }

    private static KnowledgePoint point(String id, String name, String category, String teacherId) {
        return new KnowledgePoint(id, name, "说明", null, category, teacherId, LocalDateTime.now());
    }

    private static Question question(String id, int version) {
        return new Question(id, "c1", TEACHER_ID, "切割", "题干", null, "中", List.of("kp1"),
                "{}", List.of(), "答案", "v1.2", version, "PUBLISHED");
    }

    private static AnswerRecord answer(String id, double score, String status) {
        return new AnswerRecord(id, "e1", "s1", "q1", "作答", LocalDateTime.now(), score,
                status, null, null);
    }

    private static Exercise exercise(String id) {
        return new Exercise(id, "c1", TEACHER_ID, "练习一", List.of("q1"),
                LocalDateTime.now().plusDays(1), "PUBLISHED", LocalDateTime.now());
    }

    private static Account account(String id, String name) {
        return new Account(id, id, "pw", name, "课程组", AccountStatus.ENABLED,
                List.of(new Role("TEACHER", "教师", SystemType.TEACHING)));
    }

    private static <T> PageResult<T> slice(List<T> all, int page, int size) {
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResult<>(all.subList(from, to), all.size(), page, size);
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
        private final Map<String, Account> byId = new HashMap<>();

        private void put(Account account) {
            byId.put(account.accountId(), account);
        }

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(byId.get(accountId));
        }

        @Override
        public Optional<Account> findByAccount(String account) {
            return byId.values().stream().filter(a -> a.account().equals(account)).findFirst();
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
            byId.put(account.accountId(), account);
            return account.accountId();
        }

        @Override
        public void updateStatus(String accountId, AccountStatus status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Role> findRoles(String accountId) {
            return byId.containsKey(accountId) ? byId.get(accountId).roles() : List.of();
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
        private boolean enabled = true;
        private DataScope scope = DataScope.OWN_CLASS;
        private Set<IdentityType> roles = Set.of(IdentityType.TEACHER);

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
            return enabled;
        }

        @Override
        public List<IdentityOption> availableIdentities(Account account) {
            return List.of();
        }
    }

    private static final class FakeTeachingRepository implements TeachingRepository {
        private final Map<String, KnowledgePoint> points = new HashMap<>();
        private final Map<String, LearningMaterial> materials = new HashMap<>();
        private final Map<String, Question> questions = new HashMap<>();
        private final Map<String, Exercise> exercises = new HashMap<>();
        private final Map<String, AnswerRecord> answers = new HashMap<>();
        private final Map<String, LearningStat> stats = new HashMap<>();
        private final Map<String, Integer> thresholds = new HashMap<>();
        private final Map<String, Double> scores = new HashMap<>();
        private final Map<String, String> statuses = new HashMap<>();
        private final Map<String, TeacherFeedback> feedbacks = new HashMap<>();
        private final List<QuestionVersion> savedVersions = new ArrayList<>();
        private final List<LearningRecord> learningRecords = new ArrayList<>();
        private final List<DifferenceItem> differences = new ArrayList<>();
        private final List<KnowledgePoint> savedPoints = new ArrayList<>();
        private final List<LearningMaterial> savedMaterials = new ArrayList<>();
        private final List<Question> savedQuestions = new ArrayList<>();
        private final List<Exercise> savedExercises = new ArrayList<>();
        private int seq = 10;

        @Override
        public String saveKnowledgePoint(KnowledgePoint point) {
            String id = point.pointId() == null ? "kp" + seq++ : point.pointId();
            points.put(id, new KnowledgePoint(id, point.name(), point.description(), point.parentId(),
                    point.category(), point.teacherId(), point.createdAt()));
            savedPoints.add(points.get(id));
            return id;
        }

        @Override
        public Optional<KnowledgePoint> findKnowledgePoint(String pointId) {
            return Optional.ofNullable(points.get(pointId));
        }

        @Override
        public void deleteKnowledgePoint(String pointId) {
            points.remove(pointId);
        }

        @Override
        public PageResult<KnowledgePoint> findKnowledgePoints(String teacherId, String category, int page, int size) {
            List<KnowledgePoint> found = points.values().stream()
                    .filter(p -> teacherId == null || teacherId.equals(p.teacherId()))
                    .filter(p -> category == null || category.isBlank() || category.equals(p.category()))
                    .collect(Collectors.toList());
            return slice(found, page, size);
        }

        @Override
        public String saveMaterial(LearningMaterial material) {
            String id = material.materialId() == null ? "m" + seq++ : material.materialId();
            materials.put(id, new LearningMaterial(id, material.title(), material.content(),
                    material.pointIds(), material.fileId(), material.classId(), material.status(),
                    material.createdBy(), material.createdAt()));
            savedMaterials.add(materials.get(id));
            return id;
        }

        @Override
        public Optional<LearningMaterial> findMaterial(String materialId) {
            return Optional.ofNullable(materials.get(materialId));
        }

        @Override
        public void updateMaterialStatus(String materialId, String status, String operatorId, LocalDateTime time) {
            LearningMaterial m = materials.get(materialId);
            if (m != null) {
                materials.put(materialId, new LearningMaterial(m.materialId(), m.title(), m.content(),
                        m.pointIds(), m.fileId(), m.classId(), status, m.createdBy(), m.createdAt()));
            }
        }

        @Override
        public PageResult<LearningMaterial> findMaterials(String classId, String status, int page, int size) {
            List<LearningMaterial> found = materials.values().stream()
                    .filter(m -> classId == null || classId.equals(m.classId()))
                    .filter(m -> status == null || status.isBlank() || status.equals(m.status()))
                    .collect(Collectors.toList());
            return slice(found, page, size);
        }

        @Override
        public void saveLearningRecord(LearningRecord record) {
            learningRecords.add(record);
        }

        @Override
        public Optional<LearningRecord> findLearningRecord(String studentId, String materialId) {
            return learningRecords.stream()
                    .filter(r -> r.studentId().equals(studentId) && r.materialId().equals(materialId))
                    .findFirst();
        }

        @Override
        public String saveQuestion(Question question) {
            String id = question.questionId() == null ? "q" + seq++ : question.questionId();
            questions.put(id, new Question(id, question.classId(), question.teacherId(), question.type(),
                    question.stem(), question.documentId(), question.difficulty(), question.pointIds(),
                    question.standardJson(), question.options(), question.answer(), question.guideVersion(),
                    question.version(), question.status()));
            savedQuestions.add(questions.get(id));
            return id;
        }

        @Override
        public Optional<Question> findQuestion(String questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }

        @Override
        public int saveQuestionVersion(String questionId, int version, String standardAnswer,
                                       String guideVersion, LocalDateTime effectiveAt) {
            savedVersions.add(new QuestionVersion(questionId, version, standardAnswer, guideVersion, effectiveAt));
            return version;
        }

        @Override
        public Optional<QuestionVersion> findQuestionVersion(String questionId, int version) {
            return savedVersions.stream()
                    .filter(v -> v.questionId().equals(questionId) && v.version() == version)
                    .findFirst();
        }

        @Override
        public PageResult<Question> findQuestions(String classId, String pointId, int page, int size) {
            List<Question> found = questions.values().stream()
                    .filter(q -> classId == null || classId.equals(q.classId()))
                    .filter(q -> pointId == null || pointId.isBlank() || q.pointIds().contains(pointId))
                    .collect(Collectors.toList());
            return slice(found, page, size);
        }

        @Override
        public String saveExercise(Exercise exercise) {
            String id = exercise.exerciseId() == null ? "e" + seq++ : exercise.exerciseId();
            exercises.put(id, new Exercise(id, exercise.classId(), exercise.teacherId(), exercise.name(),
                    exercise.questionIds(), exercise.deadline(), exercise.status(), exercise.createdAt()));
            savedExercises.add(exercises.get(id));
            return id;
        }

        @Override
        public Optional<Exercise> findExercise(String exerciseId) {
            return Optional.ofNullable(exercises.get(exerciseId));
        }

        @Override
        public void updateExerciseStatus(String exerciseId, String status, String operatorId, LocalDateTime time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Exercise> findExercises(String classId, String status, int page, int size) {
            List<Exercise> found = exercises.values().stream()
                    .filter(e -> classId == null || classId.equals(e.classId()))
                    .filter(e -> status == null || status.isBlank() || status.equals(e.status()))
                    .collect(Collectors.toList());
            return slice(found, page, size);
        }

        @Override
        public String saveAnswer(AnswerRecord answer, String requestId) {
            answers.put(answer.answerId(), answer);
            return answer.answerId();
        }

        @Override
        public void saveAnswerDraft(AnswerRecord draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<AnswerRecord> findAnswerDraft(String studentId, String exerciseId) {
            return Optional.empty();
        }

        @Override
        public void deleteAnswerDraft(String studentId, String exerciseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<AnswerRecord> findAnswer(String answerId) {
            return Optional.ofNullable(answers.get(answerId));
        }

        @Override
        public List<AnswerRecord> findAnswers(String studentId, String exerciseId) {
            return answers.values().stream()
                    .filter(a -> studentId == null || studentId.equals(a.studentId()))
                    .filter(a -> exerciseId == null || exerciseId.equals(a.exerciseId()))
                    .collect(Collectors.toList());
        }

        @Override
        public void updateAnswerStatus(String answerId, String status, String feedback, String teacherId,
                                       LocalDateTime time) {
            statuses.put(answerId, status);
        }

        @Override
        public void updateAnswerScore(String answerId, double score) {
            scores.put(answerId, score);
        }

        @Override
        public void saveDifference(String answerId, List<DifferenceItem> items) {
            differences.addAll(items);
        }

        @Override
        public List<DifferenceItem> findDifferences(String answerId, String questionId) {
            return differences;
        }

        @Override
        public void saveTeacherFeedback(String answerId, String teacherId, String content, LocalDateTime time) {
            feedbacks.put(answerId, new TeacherFeedback(answerId, teacherId, content, time));
        }

        @Override
        public Optional<TeacherFeedback> findTeacherFeedback(String answerId) {
            return Optional.ofNullable(feedbacks.get(answerId));
        }

        @Override
        public LearningStat findLearningStat(String studentId, String classId, String pointId,
                                             String exerciseId, LocalDateTime from, LocalDateTime to) {
            if (studentId == null && pointId != null) {
                return stats.getOrDefault(pointId, new LearningStat(0, 0, 0, "未开始"));
            }
            return stats.getOrDefault(studentId, new LearningStat(0, 0, 0, "未开始"));
        }

        @Override
        public void saveWeakThreshold(String classId, int threshold) {
            thresholds.put(classId, threshold);
        }

        @Override
        public Optional<Integer> findWeakThreshold(String classId) {
            return Optional.ofNullable(thresholds.get(classId));
        }
    }

    private static final class FakeTeachingAdminRepository implements TeachingAdminRepository {
        private final Map<String, TeachingClass> classes = new HashMap<>();
        private final List<ClassMember> members = new ArrayList<>();

        @Override
        public String saveClass(TeachingClass teachingClass) {
            classes.put(teachingClass.classId(), teachingClass);
            return teachingClass.classId();
        }

        @Override
        public Optional<TeachingClass> findClass(String classId) {
            return Optional.ofNullable(classes.get(classId));
        }

        @Override
        public Optional<TeachingClass> findClassByCode(String classCode) {
            return classes.values().stream().filter(c -> c.classCode().equals(classCode)).findFirst();
        }

        @Override
        public PageResult<TeachingClass> findClasses(String term, String teacherId, int page, int size) {
            List<TeachingClass> found = classes.values().stream()
                    .filter(c -> teacherId == null || teacherId.equals(c.teacherId()))
                    .collect(Collectors.toList());
            return slice(found, page, size);
        }

        @Override
        public void saveClassMember(String classId, String studentId, String studentNo, LocalDateTime joinedAt,
                                    String status) {
            members.add(new ClassMember(classId, studentId, studentNo, joinedAt, status));
        }

        @Override
        public List<ClassMember> findClassMembers(String classId) {
            return members.stream().filter(m -> m.classId().equals(classId)).collect(Collectors.toList());
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
            return Optional.empty();
        }

        @Override
        public Optional<TeachingClass> findClassByStudentId(String studentId) {
            return members.stream().filter(m -> m.studentId().equals(studentId))
                    .map(m -> classes.get(m.classId())).findFirst();
        }

        @Override
        public Optional<Account> findByStudentNo(String studentNo) {
            return Optional.empty();
        }

        @Override
        public Optional<Account> findByTeacherNo(String teacherNo) {
            return Optional.empty();
        }

        @Override
        public StudentAccountResult createStudentAccount(String studentNo, String name, String classId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeGuideRepository implements GuideRepository {
        private final List<List<String>> savedReferences = new ArrayList<>();

        @Override
        public Optional<GuideVersion> findVersion(String version) {
            return Optional.empty();
        }

        @Override
        public List<GuideVersion> findAvailable() {
            return List.of();
        }

        @Override
        public String saveVersion(GuideVersion version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveReference(String objectType, String objectId, String version) {
            savedReferences.add(List.of(objectType, objectId, version));
        }

        @Override
        public Optional<String> findReference(String objectType, String objectId) {
            return savedReferences.stream()
                    .filter(r -> r.get(0).equals(objectType) && r.get(1).equals(objectId))
                    .map(r -> r.get(2)).findFirst();
        }

        @Override
        public void saveReadingRecord(ReadingRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ReadingRecord> findReadingRecord(String userId, String version) {
            return Optional.empty();
        }
    }

    private static final class FakeFileRepository implements FileRepository {
        private final Map<String, StoredFile> files = new HashMap<>();

        @Override
        public String save(String fileName, String format, byte[] content, String uploaderId,
                           LocalDateTime time, String type) {
            String id = "f" + files.size();
            files.put(id, new StoredFile(id, fileName, format, content, uploaderId, time, type, null));
            return id;
        }

        @Override
        public Optional<StoredFile> find(String fileId) {
            return Optional.ofNullable(files.get(fileId));
        }

        @Override
        public void delete(String fileId) {
            files.remove(fileId);
        }
    }

    private static final class FakeTeachingRule implements TeachingRule {
        private boolean uniqueName = true;
        private List<String> materialMissing = List.of();
        private List<String> questionMissing = List.of();
        private Integer nextVersion;
        private List<String> weakPoints = List.of();
        private List<Question> targeted = List.of();
        private int lastWeakThreshold;

        @Override
        public boolean uniquePointName(String name, List<KnowledgePoint> points) {
            return uniqueName;
        }

        @Override
        public List<String> checkMaterialPublish(LearningMaterial material, String guideVersion, String classId) {
            return materialMissing;
        }

        @Override
        public List<String> checkQuestion(String questionType, QuestionVersion version) {
            return questionMissing;
        }

        @Override
        public int nextVersion(Question question) {
            return nextVersion != null ? nextVersion : question.version() + 1;
        }

        @Override
        public Score score(String answerContent, String standardAnswer, String questionType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double average(List<Double> scores) {
            return 0;
        }

        @Override
        public String completion(List<AnswerRecord> answers) {
            return "";
        }

        @Override
        public KnowledgeStat summarize(List<AnswerRecord> answers, List<String> pointIds) {
            return new KnowledgeStat(pointIds, List.of(), List.of(), List.of());
        }

        @Override
        public List<String> weakPoints(KnowledgeStat stat, int threshold) {
            lastWeakThreshold = threshold;
            return weakPoints;
        }

        @Override
        public List<Question> targetedQuestions(List<String> pointIds, List<Question> pool) {
            return targeted;
        }

        @Override
        public long studyDuration(LocalDateTime first, LocalDateTime last, List<LocalDateTime> leaves) {
            return 0;
        }
    }

    private static final class FakeNotificationService implements NotificationService {
        private record Sent(String receiverId, String content, String type) {
        }

        private final List<Sent> sent = new ArrayList<>();

        @Override
        public String sendInApp(String receiverId, String content, String type) {
            sent.add(new Sent(receiverId, content, type));
            return "n" + sent.size();
        }

        @Override
        public boolean sendMail(String receiver, String subject, String content) {
            return true;
        }
    }
}
