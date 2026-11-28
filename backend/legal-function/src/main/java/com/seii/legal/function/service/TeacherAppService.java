package com.seii.legal.function.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.enums.DataScope;
import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.ClassMember;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.KnowledgePoint;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.LearningStat;
import com.seii.legal.data.entity.Question;
import com.seii.legal.data.entity.QuestionVersion;
import com.seii.legal.data.entity.TeachingClass;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.data.repository.TeachingRepository;
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
import com.seii.legal.function.dto.teacher.StudentLearningView;
import com.seii.legal.function.dto.teacher.TargetedPointView;
import com.seii.legal.function.dto.teacher.TargetedView;
import com.seii.legal.function.dto.teacher.ThresholdCommand;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.notification.NotificationService;
import com.seii.legal.rule.teaching.TeachingRule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherAppService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int DEFAULT_THRESHOLD = 60;
    private static final int MAX_SIZE = 10000;

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final IdentityRule identityRule;
    private final TeachingRepository teachingRepository;
    private final GuideRepository guideRepository;
    private final FileRepository fileRepository;
    private final TeachingAdminRepository teachingAdminRepository;
    private final TeachingRule teachingRule;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public TeacherAppService(SessionRepository sessionRepository,
                             AccountRepository accountRepository,
                             IdentityRule identityRule,
                             TeachingRepository teachingRepository,
                             GuideRepository guideRepository,
                             FileRepository fileRepository,
                             TeachingAdminRepository teachingAdminRepository,
                             TeachingRule teachingRule,
                             NotificationService notificationService,
                             ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.identityRule = identityRule;
        this.teachingRepository = teachingRepository;
        this.guideRepository = guideRepository;
        this.fileRepository = fileRepository;
        this.teachingAdminRepository = teachingAdminRepository;
        this.teachingRule = teachingRule;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<KnowledgePoint>> points(String credential, String category, int page, int size) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        return AppResult.ok(teachingRepository.findKnowledgePoints(auth.accountId(), blankToNull(category),
                normalizePage(page), normalizeSize(size)));
    }

    @Transactional
    public AppResult<IdResult> savePoint(String credential, KnowledgePointCommand command) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        KnowledgePoint existing = null;
        if (!blank(command.id())) {
            existing = teachingRepository.findKnowledgePoint(command.id()).orElse(null);
            if (existing == null) {
                return AppResult.fail("知识点不存在");
            }
            if (!auth.accountId().equals(existing.teacherId())) {
                return AppResult.fail("只能修改自己的知识点");
            }
        }
        KnowledgePoint current = existing;
        List<KnowledgePoint> points = teachingRepository
                .findKnowledgePoints(auth.accountId(), null, 1, MAX_SIZE).records().stream()
                .filter(p -> current == null || !p.pointId().equals(current.pointId()))
                .collect(Collectors.toList());
        if (!teachingRule.uniquePointName(command.name(), points)) {
            return AppResult.fail("知识点名称已经存在");
        }
        LocalDateTime now = LocalDateTime.now();
        KnowledgePoint point = new KnowledgePoint(
                command.id(),
                command.name(),
                command.description(),
                existing == null ? null : existing.parentId(),
                command.category(),
                auth.accountId(),
                existing == null ? now : existing.createdAt());
        return AppResult.ok(new IdResult(teachingRepository.saveKnowledgePoint(point)));
    }

    @Transactional
    public AppResult<Void> deletePoint(String credential, PointDeleteCommand command) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        KnowledgePoint point = teachingRepository.findKnowledgePoint(command.id()).orElse(null);
        if (point == null) {
            return AppResult.fail("知识点不存在");
        }
        if (!auth.accountId().equals(point.teacherId())) {
            return AppResult.fail("只能修改自己的知识点");
        }
        teachingRepository.deleteKnowledgePoint(command.id());
        return AppResult.ok(null);
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<LearningMaterial>> materials(String credential, String status, int page, int size) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        List<PageResult<LearningMaterial>> pages = teacherClassIds(auth.accountId()).stream()
                .map(classId -> teachingRepository.findMaterials(classId, blankToNull(status), 1, MAX_SIZE))
                .collect(Collectors.toList());
        return AppResult.ok(merge(pages, normalizePage(page), normalizeSize(size)));
    }

    @Transactional
    public AppResult<IdResult> saveMaterial(String credential, MaterialCommand command) {
        Auth auth = authenticate(credential, command.classId());
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        LearningMaterial existing = null;
        if (!blank(command.id())) {
            existing = teachingRepository.findMaterial(command.id()).orElse(null);
            if (existing == null) {
                return AppResult.fail("学习材料不存在");
            }
            if (!auth.accountId().equals(existing.createdBy())) {
                return AppResult.fail("只能修改自己的学习材料");
            }
        }
        if (!blank(command.fileId()) && fileRepository.find(command.fileId()).isEmpty()) {
            return AppResult.fail("文件不存在");
        }
        if (!blank(command.classId()) && !ownsClass(auth.accountId(), command.classId())) {
            return AppResult.fail("只能操作自己班级的学习材料");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean publish = Boolean.TRUE.equals(command.publish());
        String status = publish ? "已发布" : existing == null ? "草稿" : existing.status();
        LearningMaterial material = new LearningMaterial(
                command.id(),
                command.title(),
                command.content(),
                command.pointIds() == null ? List.of() : command.pointIds(),
                command.fileId(),
                command.classId(),
                status,
                auth.accountId(),
                existing == null ? now : existing.createdAt());
        if (publish) {
            List<String> missing = teachingRule.checkMaterialPublish(material, command.guideVersion(),
                    command.classId());
            if (missing != null && !missing.isEmpty()) {
                return AppResult.fail("缺少发布条件：" + String.join("、", missing));
            }
        }
        String id = teachingRepository.saveMaterial(material);
        if (!blank(command.guideVersion())) {
            guideRepository.saveReference("MATERIAL", id, command.guideVersion());
        }
        return AppResult.ok(new IdResult(id));
    }

    @Transactional
    public AppResult<Void> updateMaterialStatus(String credential, MaterialStatusCommand command) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        LearningMaterial material = teachingRepository.findMaterial(command.id()).orElse(null);
        if (material == null) {
            return AppResult.fail("学习材料不存在");
        }
        if (!auth.accountId().equals(material.createdBy())) {
            return AppResult.fail("只能修改自己的学习材料");
        }
        teachingRepository.updateMaterialStatus(command.id(), command.status(), auth.accountId(), LocalDateTime.now());
        return AppResult.ok(null);
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<Question>> questions(String credential, String pointId, int page, int size) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        List<PageResult<Question>> pages = teacherClassIds(auth.accountId()).stream()
                .map(classId -> teachingRepository.findQuestions(classId, blankToNull(pointId), 1, MAX_SIZE))
                .collect(Collectors.toList());
        return AppResult.ok(merge(pages, normalizePage(page), normalizeSize(size)));
    }

    @Transactional
    public AppResult<QuestionResult> saveQuestion(String credential, QuestionCommand command) {
        Auth auth = authenticate(credential, command.classId());
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        Question existing = null;
        if (!blank(command.id())) {
            existing = teachingRepository.findQuestion(command.id()).orElse(null);
            if (existing == null) {
                return AppResult.fail("题目不存在");
            }
            if (!auth.accountId().equals(existing.teacherId())) {
                return AppResult.fail("只能修改自己的题目");
            }
        }
        if (!blank(command.classId()) && !ownsClass(auth.accountId(), command.classId())) {
            return AppResult.fail("只能操作自己班级的题目");
        }
        String standardJson = serializeStandard(command.standard());
        String guideVersion = blank(command.guideVersion())
                ? existing == null ? null : existing.guideVersion()
                : command.guideVersion();
        Question base = existing == null
                ? new Question(null, command.classId(), auth.accountId(), command.type(), command.stem(),
                        command.documentId(), command.difficulty(),
                        command.pointIds() == null ? List.of() : command.pointIds(), standardJson,
                        command.options() == null ? List.of() : command.options(), command.answer(),
                        guideVersion, 0, "PUBLISHED")
                : existing;
        LocalDateTime now = LocalDateTime.now();
        QuestionVersion versionInfo = new QuestionVersion(command.id(), base.version() + 1, standardJson,
                guideVersion, now);
        List<String> missing = teachingRule.checkQuestion(command.type(), versionInfo);
        if (missing != null && !missing.isEmpty()) {
            return AppResult.fail("题目缺少：" + String.join("、", missing));
        }
        int version = teachingRule.nextVersion(base);
        Question question = new Question(
                command.id(),
                blank(command.classId()) ? base.classId() : command.classId(),
                auth.accountId(),
                command.type(),
                command.stem(),
                command.documentId(),
                command.difficulty(),
                command.pointIds() == null ? List.of() : command.pointIds(),
                standardJson,
                command.options() == null ? List.of() : command.options(),
                command.answer(),
                guideVersion,
                version,
                existing == null ? "PUBLISHED" : existing.status());
        String id = teachingRepository.saveQuestion(question);
        teachingRepository.saveQuestionVersion(id, version, standardJson, guideVersion, now);
        return AppResult.ok(new QuestionResult(id, version));
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<Exercise>> exercises(String credential, String status, int page, int size) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        List<PageResult<Exercise>> pages = teacherClassIds(auth.accountId()).stream()
                .map(classId -> teachingRepository.findExercises(classId, blankToNull(status), 1, MAX_SIZE))
                .collect(Collectors.toList());
        return AppResult.ok(merge(pages, normalizePage(page), normalizeSize(size)));
    }

    @Transactional
    public AppResult<IdResult> saveExercise(String credential, ExerciseCommand command) {
        Auth auth = authenticate(credential, command.classId());
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        if (command.questionIds() == null || command.questionIds().isEmpty()) {
            return AppResult.fail("请至少选择一道题目");
        }
        Exercise existing = null;
        if (!blank(command.id())) {
            existing = teachingRepository.findExercise(command.id()).orElse(null);
            if (existing == null) {
                return AppResult.fail("练习任务不存在");
            }
            if (!auth.accountId().equals(existing.teacherId())) {
                return AppResult.fail("只能修改自己的练习任务");
            }
        }
        if (!blank(command.classId()) && !ownsClass(auth.accountId(), command.classId())) {
            return AppResult.fail("只能操作自己班级的练习任务");
        }
        Exercise exercise = new Exercise(
                command.id(),
                command.classId(),
                auth.accountId(),
                command.name(),
                command.questionIds(),
                command.deadline(),
                existing == null ? "PUBLISHED" : existing.status(),
                existing == null ? LocalDateTime.now() : existing.createdAt());
        String id = teachingRepository.saveExercise(exercise);
        if (!blank(command.guideVersion())) {
            guideRepository.saveReference("EXERCISE", id, command.guideVersion());
        }
        return AppResult.ok(new IdResult(id));
    }

    @Transactional(readOnly = true)
    public AppResult<ClassLearningView> classLearning(String credential, String classId, String studentId,
                                                      String pointId, String exerciseId) {
        Auth auth = authenticate(credential, classId);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        String ownershipError = classOwnershipError(auth.accountId(), classId, "只能查看自己班级的学习情况");
        if (ownershipError != null) {
            return AppResult.fail(ownershipError);
        }
        int threshold = teachingRepository.findWeakThreshold(classId).orElse(DEFAULT_THRESHOLD);
        List<StudentLearningView> students = new ArrayList<>();
        for (ClassMember member : teachingAdminRepository.findClassMembers(classId)) {
            if (!blank(studentId) && !studentId.equals(member.studentId())) {
                continue;
            }
            LearningStat stat = teachingRepository.findLearningStat(member.studentId(), classId,
                    blankToNull(pointId), blankToNull(exerciseId), null, null);
            if (stat == null) {
                stat = new LearningStat(0, 0, 0, "未开始");
            }
            String name = accountRepository.findById(member.studentId())
                    .map(Account::name).orElse(member.studentId());
            List<String> weak = blank(pointId) ? List.of() : teachingRule.weakPoints(
                    new TeachingRule.KnowledgeStat(List.of(pointId), List.of(stat.practiceCount()),
                            List.of(stat.bestScore()), List.of(stat.latestScore())), threshold);
            students.add(new StudentLearningView(member.studentId(), name, stat.practiceCount(),
                    stat.bestScore(), stat.latestScore(), stat.completionStatus(), weak));
        }
        return AppResult.ok(new ClassLearningView(students, threshold));
    }

    @Transactional
    public AppResult<Void> saveThreshold(String credential, ThresholdCommand command) {
        Auth auth = authenticate(credential, command.classId());
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        String ownershipError = classOwnershipError(auth.accountId(), command.classId(), "只能设置自己班级的薄弱阈值");
        if (ownershipError != null) {
            return AppResult.fail(ownershipError);
        }
        int threshold = command.threshold() == null ? DEFAULT_THRESHOLD : command.threshold();
        teachingRepository.saveWeakThreshold(command.classId(), threshold);
        return AppResult.ok(null);
    }

    @Transactional(readOnly = true)
    public AppResult<TargetedView> targeted(String credential, String classId, String pointIds) {
        Auth auth = authenticate(credential, classId);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        String ownershipError = classOwnershipError(auth.accountId(), classId, "只能查看自己班级的针对性练习");
        if (ownershipError != null) {
            return AppResult.fail(ownershipError);
        }
        List<String> ids = splitIds(pointIds);
        if (ids.isEmpty()) {
            return AppResult.ok(new TargetedView(List.of(), List.of()));
        }
        int threshold = teachingRepository.findWeakThreshold(classId).orElse(DEFAULT_THRESHOLD);
        List<Integer> practiceCounts = new ArrayList<>();
        List<Double> bestScores = new ArrayList<>();
        List<Double> recentScores = new ArrayList<>();
        Map<String, Double> bestByPoint = new LinkedHashMap<>();
        for (String pointId : ids) {
            LearningStat stat = teachingRepository.findLearningStat(null, classId, pointId, null, null, null);
            if (stat == null) {
                stat = new LearningStat(0, 0, 0, "未开始");
            }
            practiceCounts.add(stat.practiceCount());
            bestScores.add(stat.bestScore());
            recentScores.add(stat.latestScore());
            bestByPoint.put(pointId, stat.bestScore());
        }
        List<String> weak = teachingRule.weakPoints(new TeachingRule.KnowledgeStat(ids, practiceCounts,
                bestScores, recentScores), threshold);
        List<TargetedPointView> points = weak.stream()
                .map(pointId -> new TargetedPointView(pointId, bestByPoint.getOrDefault(pointId, 0.0)))
                .sorted(Comparator.comparingDouble(TargetedPointView::bestScore))
                .collect(Collectors.toList());
        List<Question> pool = teachingRepository.findQuestions(classId, null, 1, MAX_SIZE).records();
        return AppResult.ok(new TargetedView(points, teachingRule.targetedQuestions(weak, pool)));
    }

    @Transactional
    public AppResult<Void> feedback(String credential, FeedbackCommand command) {
        Auth auth = authenticate(credential, null);
        if (!auth.ok()) {
            return AppResult.fail(auth.message());
        }
        AnswerRecord answer = teachingRepository.findAnswer(command.answerId()).orElse(null);
        if (answer == null) {
            return AppResult.fail("作答记录不存在");
        }
        Exercise exercise = teachingRepository.findExercise(answer.exerciseId()).orElse(null);
        if (exercise != null && !blank(exercise.classId())
                && !ownsClass(auth.accountId(), exercise.classId())) {
            return AppResult.fail("只能评价自己班级的作答");
        }
        if (command.score() == null && answer.score() <= 0) {
            return AppResult.fail("请先录入得分");
        }
        if (blank(command.feedback())) {
            return AppResult.fail("请填写反馈内容");
        }
        String content = command.feedback();
        if (command.score() != null) {
            teachingRepository.updateAnswerScore(command.answerId(), command.score());
        }
        LocalDateTime now = LocalDateTime.now();
        teachingRepository.updateAnswerStatus(command.answerId(), "已批改", content, auth.accountId(), now);
        teachingRepository.saveTeacherFeedback(command.answerId(), auth.accountId(), content, now);
        String notice = command.score() == null
                ? "教师反馈：" + content
                : "教师反馈：" + content + "，得分：" + command.score();
        notificationService.sendInApp(answer.studentId(), notice, "TEACHER_FEEDBACK");
        return AppResult.ok(null);
    }

    private Auth authenticate(String credential, String classId) {
        LoginUser user = sessionRepository.findByCredential(credential).orElse(null);
        if (user == null) {
            return new Auth(false, "登录已失效，请重新登录", null);
        }
        Account account = accountRepository.findById(user.accountId()).orElse(null);
        if (account == null) {
            return new Auth(false, "账号不存在", null);
        }
        if (!identityRule.isEnabled(account)) {
            return new Auth(false, "账号已停用，请联系管理员", null);
        }
        if (user.identity() != IdentityType.TEACHER || !identityRule.hasRole(account, IdentityType.TEACHER)) {
            return new Auth(false, "只有教师可以访问教学功能", null);
        }
        DataScope scope = identityRule.resolveScope(user, null, classId);
        if (scope != DataScope.OWN_CLASS) {
            return new Auth(false, "当前身份不能访问这个班级", null);
        }
        return new Auth(true, "成功", account.accountId());
    }

    private boolean ownsClass(String teacherId, String classId) {
        return teachingAdminRepository.findClass(classId)
                .map(teachingClass -> teacherId.equals(teachingClass.teacherId()))
                .orElse(false);
    }

    private String classOwnershipError(String teacherId, String classId, String forbiddenMessage) {
        if (blank(classId)) {
            return "请选择班级";
        }
        TeachingClass teachingClass = teachingAdminRepository.findClass(classId).orElse(null);
        if (teachingClass == null) {
            return "班级不存在";
        }
        if (!teacherId.equals(teachingClass.teacherId())) {
            return forbiddenMessage;
        }
        return null;
    }

    private List<String> teacherClassIds(String teacherId) {
        return teachingAdminRepository.findClasses(null, teacherId, 1, MAX_SIZE).records().stream()
                .map(TeachingClass::classId)
                .collect(Collectors.toList());
    }

    private <T> PageResult<T> merge(List<PageResult<T>> pages, int page, int size) {
        List<T> all = pages.stream().flatMap(result -> result.records().stream()).collect(Collectors.toList());
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResult<>(all.subList(from, to), all.size(), page, size);
    }

    private String serializeStandard(Object standard) {
        if (standard == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(standard);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("标准答案序列化失败");
        }
    }

    private static List<String> splitIds(String value) {
        if (blank(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
    }

    private static int normalizePage(int page) {
        return page <= 0 ? DEFAULT_PAGE : page;
    }

    private static int normalizeSize(int size) {
        return size <= 0 ? DEFAULT_SIZE : size;
    }

    private static String blankToNull(String value) {
        return blank(value) ? null : value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record Auth(boolean ok, String message, String accountId) {
    }
}
