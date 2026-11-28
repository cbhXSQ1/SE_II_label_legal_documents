package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
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
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.data.repository.TeachingRepository;
import com.seii.legal.function.dto.TeacherDtos;
import com.seii.legal.rule.guide.GuideRule;
import com.seii.legal.rule.teaching.TeachingRule;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 教师教学功能层服务（TCH-01…14）。
 */
@Service
@RequiredArgsConstructor
public class TeacherAppService {

    private static final Set<IdentityType> TEACHER_ONLY = Set.of(IdentityType.TEACHER);
    private static final int DEFAULT_WEAK_THRESHOLD = 60;

    private final AuthActionTemplate authTemplate;
    private final TeachingRepository teachingRepository;
    private final TeachingAdminRepository teachingAdminRepository;
    private final AccountRepository accountRepository;
    private final GuideRepository guideRepository;
    private final TeachingRule teachingRule;
    private final GuideRule guideRule;
    private final AnnotationPayloadAdapter jsonHelper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AppResult<PageResult<TeacherDtos.KnowledgePointItem>> listPoints(
            String credential, String category, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, null, "仅教师可管理知识点", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            PageResult<KnowledgePoint> raw = teachingRepository.findKnowledgePoints(ctx.user().accountId(), category, p, s);
            List<TeacherDtos.KnowledgePointItem> items = raw.records().stream()
                    .map(kp -> new TeacherDtos.KnowledgePointItem(
                            kp.pointId(), kp.name(), kp.category(), kp.description(), kp.difficulty(), 0, 0))
                    .toList();
            return AppResult.ok(new PageResult<>(items, raw.total(), raw.page(), raw.size()));
        });
    }

    @Transactional
    public AppResult<TeacherDtos.IdResponse> savePoint(String credential, TeacherDtos.SavePointRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, null, "仅教师可维护知识点", ctx -> {
            List<KnowledgePoint> existing = teachingRepository.findKnowledgePoints(ctx.user().accountId(), null, 1, 500)
                    .records().stream()
                    .filter(kp -> request.id() == null || !request.id().equals(kp.pointId()))
                    .toList();
            if (!teachingRule.uniquePointName(request.name().trim(), existing)) {
                return AppResult.fail("知识点名称已经存在");
            }
            KnowledgePoint point = new KnowledgePoint(
                    request.id(),
                    ctx.user().accountId(),
                    request.name().trim(),
                    request.category().trim(),
                    request.description(),
                    request.difficulty()
            );
            String id = teachingRepository.saveKnowledgePoint(point);
            return AppResult.ok(new TeacherDtos.IdResponse(id));
        });
    }

    @Transactional
    public AppResult<Void> deletePoint(String credential, TeacherDtos.DeletePointRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, null, "仅教师可删除知识点", ctx -> {
            KnowledgePoint existing = teachingRepository.findKnowledgePoint(request.id())
                    .orElseThrow(() -> new BusinessException("知识点不存在"));
            if (!ctx.user().accountId().equals(existing.teacherId())) {
                return AppResult.fail("只能删除本人创建的知识点");
            }
            teachingRepository.deleteKnowledgePoint(request.id());
            return AppResult.ok(null);
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<LearningMaterial>> listMaterials(
            String credential, String classId, String status, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, classId, "仅教师可查看学习材料", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), classId);
            return AppResult.ok(teachingRepository.findMaterials(effectiveClassId, status, p, s));
        });
    }

    @Transactional
    public AppResult<TeacherDtos.IdResponse> saveMaterial(String credential, TeacherDtos.SaveMaterialRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, request.classId(), "仅教师可编辑学习材料", ctx -> {
            LocalDateTime now = TimeFormats.now();
            String status = request.publish() ? "已发布" : "草稿";
            LearningMaterial material = new LearningMaterial(
                    request.id(),
                    ctx.user().accountId(),
                    request.title().trim(),
                    request.type().trim(),
                    request.content(),
                    request.fileId(),
                    request.guideVersion(),
                    request.pointIds() == null ? List.of() : request.pointIds(),
                    request.classId(),
                    status,
                    request.publish() ? now : null
            );
            if (request.publish()) {
                List<String> missing = teachingRule.checkMaterialPublish(material, request.guideVersion(), request.classId());
                if (missing != null && !missing.isEmpty()) {
                    return AppResult.fail("发布材料缺少必填项：" + String.join("、", missing));
                }
            }
            String id = teachingRepository.saveMaterial(material);
            if (request.guideVersion() != null && !request.guideVersion().isBlank()) {
                guideRepository.saveReference("MATERIAL", id, request.guideVersion());
                guideRule.registerReference("MATERIAL", id, request.guideVersion());
            }
            return AppResult.ok(new TeacherDtos.IdResponse(id));
        });
    }

    @Transactional
    public AppResult<Void> updateMaterialStatus(String credential, TeacherDtos.MaterialStatusRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, null, "仅教师可发布或下架材料", ctx -> {
            LearningMaterial existing = teachingRepository.findMaterial(request.id())
                    .orElseThrow(() -> new BusinessException("学习材料不存在"));
            if (!ctx.user().accountId().equals(existing.teacherId())) {
                return AppResult.fail("只能操作本人创建的学习材料");
            }
            teachingRepository.updateMaterialStatus(request.id(), request.status(), ctx.user().accountId(), TimeFormats.now());
            return AppResult.ok(null);
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<Question>> listQuestions(
            String credential, String classId, String pointId, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, classId, "仅教师可查看题目池", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), classId);
            return AppResult.ok(teachingRepository.findQuestions(effectiveClassId, pointId, p, s));
        });
    }

    @Transactional
    public AppResult<TeacherDtos.SaveQuestionResponse> saveQuestion(
            String credential, TeacherDtos.SaveQuestionRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, request.classId(), "仅教师可维护题目", ctx -> {
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), request.classId());
            String standardJson = request.standard() != null
                    ? jsonHelper.writeJson(request.standard())
                    : (request.answer() == null ? "" : request.answer());

            int nextVer = 1;
            if (request.id() != null && !request.id().isBlank()) {
                Question prev = teachingRepository.findQuestion(request.id())
                        .orElseThrow(() -> new BusinessException("题目不存在"));
                if (!ctx.user().accountId().equals(prev.teacherId())) {
                    return AppResult.fail("只能修改本人创建的题目");
                }
                nextVer = teachingRule.nextVersion(prev);
            }

            LocalDateTime now = TimeFormats.now();
            QuestionVersion qv = new QuestionVersion(request.id(), nextVer, standardJson, request.guideVersion(), now);
            List<String> missing = teachingRule.checkQuestion(request.type(), qv);
            if (missing != null && !missing.isEmpty()) {
                return AppResult.fail("题目缺少必要内容：" + String.join("、", missing));
            }
            if (request.pointIds() == null || request.pointIds().isEmpty()) {
                return AppResult.fail("请至少关联一个知识点");
            }

            Question question = new Question(
                    request.id(),
                    effectiveClassId,
                    ctx.user().accountId(),
                    request.type(),
                    request.stem(),
                    request.documentId(),
                    request.difficulty(),
                    request.pointIds(),
                    standardJson,
                    request.options() == null ? List.of() : request.options(),
                    request.answer(),
                    request.guideVersion(),
                    nextVer,
                    "ACTIVE"
            );
            String questionId = teachingRepository.saveQuestion(question);
            int savedVersion = teachingRepository.saveQuestionVersion(
                    questionId, nextVer, standardJson, request.guideVersion(), now);
            return AppResult.ok(new TeacherDtos.SaveQuestionResponse(questionId, savedVersion));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<Exercise>> listExercises(
            String credential, String classId, String status, Integer page, Integer size) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, classId, "仅教师可查看练习任务", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), classId);
            return AppResult.ok(teachingRepository.findExercises(effectiveClassId, status, p, s));
        });
    }

    @Transactional
    public AppResult<TeacherDtos.IdResponse> saveExercise(String credential, TeacherDtos.SaveExerciseRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, request.classId(), "仅教师可创建练习任务", ctx -> {
            if (request.questionIds() == null || request.questionIds().isEmpty()) {
                return AppResult.fail("请至少选择一道题目");
            }
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), request.classId());
            LocalDateTime now = TimeFormats.now();
            Exercise exercise = new Exercise(
                    request.id(),
                    ctx.user().accountId(),
                    request.name().trim(),
                    request.description(),
                    TimeFormats.parse(request.deadline()),
                    request.allowRedo(),
                    request.guideVersion(),
                    effectiveClassId,
                    request.questionIds(),
                    "已发布",
                    now
            );
            String id = teachingRepository.saveExercise(exercise);
            if (request.guideVersion() != null && !request.guideVersion().isBlank()) {
                guideRepository.saveReference("EXERCISE", id, request.guideVersion());
                guideRule.registerReference("EXERCISE", id, request.guideVersion());
            }
            return AppResult.ok(new TeacherDtos.IdResponse(id));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TeacherDtos.ClassLearningResponse> classLearning(
            String credential, String classId, String studentId, String pointId, String exerciseId) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, classId, "仅教师可查看班级学习情况", ctx -> {
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), classId);
            int threshold = teachingRepository.findWeakThreshold(effectiveClassId).orElse(DEFAULT_WEAK_THRESHOLD);
            List<ClassMember> members = teachingAdminRepository.findClassMembers(effectiveClassId);
            List<String> pointIds = teachingRepository.findKnowledgePoints(ctx.user().accountId(), null, 1, 200)
                    .records().stream().map(KnowledgePoint::pointId).toList();

            List<TeacherDtos.StudentLearningSummary> summaries = new ArrayList<>();
            for (ClassMember member : members) {
                if (studentId != null && !studentId.isBlank() && !studentId.equals(member.studentId())) {
                    continue;
                }
                List<AnswerRecord> answers = teachingRepository.findAnswers(member.studentId(), exerciseId);
                List<Double> scores = answers.stream()
                        .filter(a -> a.score() != null)
                        .map(AnswerRecord::score)
                        .toList();
                TeachingRule.KnowledgeStat stat = teachingRule.summarize(answers, pointIds);
                List<String> weakPoints = stat == null ? List.of() : teachingRule.weakPoints(stat, threshold);
                LearningStat ls = teachingRepository.findLearningStat(
                        member.studentId(), effectiveClassId, pointId, exerciseId, null, null);
                int exercisesDone = ls != null ? ls.practiceCount() : answers.size();
                summaries.add(new TeacherDtos.StudentLearningSummary(
                        member.studentId(), resolveUserName(member.studentId()), 0, exercisesDone, scores, weakPoints));
            }
            return AppResult.ok(new TeacherDtos.ClassLearningResponse(summaries, threshold));
        });
    }

    @Transactional
    public AppResult<Void> saveThreshold(String credential, TeacherDtos.SaveThresholdRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, request.classId(), "仅教师可设置薄弱阈值", ctx -> {
            if (request.threshold() < 0 || request.threshold() > 100) {
                return AppResult.fail("薄弱阈值必须在 0 到 100 之间");
            }
            verifyOwnedClass(ctx.user().accountId(), request.classId());
            teachingRepository.saveWeakThreshold(request.classId(), request.threshold());
            return AppResult.ok(null);
        });
    }

    @Transactional(readOnly = true)
    public AppResult<TeacherDtos.TargetedPracticeResponse> targetedPractice(
            String credential, String classId, List<String> pointIds) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, classId, "仅教师可创建针对性练习", ctx -> {
            String effectiveClassId = resolveTeacherClassId(ctx.user().accountId(), classId);
            int threshold = teachingRepository.findWeakThreshold(effectiveClassId).orElse(DEFAULT_WEAK_THRESHOLD);
            List<String> targetPoints = pointIds;
            if (targetPoints == null || targetPoints.isEmpty()) {
                List<String> allPoints = teachingRepository.findKnowledgePoints(ctx.user().accountId(), null, 1, 200)
                        .records().stream().map(KnowledgePoint::pointId).toList();
                List<ClassMember> members = teachingAdminRepository.findClassMembers(effectiveClassId);
                List<AnswerRecord> classAnswers = new ArrayList<>();
                for (ClassMember m : members) {
                    classAnswers.addAll(teachingRepository.findAnswers(m.studentId(), null));
                }
                TeachingRule.KnowledgeStat stat = teachingRule.summarize(classAnswers, allPoints);
                targetPoints = stat == null ? List.of() : teachingRule.weakPoints(stat, threshold);
            }
            List<Question> pool = teachingRepository.findQuestions(effectiveClassId, null, 1, 500).records();
            List<Question> matched = teachingRule.targetedQuestions(targetPoints, pool);
            return AppResult.ok(new TeacherDtos.TargetedPracticeResponse(targetPoints, matched));
        });
    }

    @Transactional
    public AppResult<Void> feedback(String credential, TeacherDtos.TeacherFeedbackRequest request) {
        return authTemplate.execute(credential, TEACHER_ONLY, null, null, "仅教师可填写反馈", ctx -> {
            if (request.feedback() == null || request.feedback().isBlank()) {
                return AppResult.fail("请填写反馈内容");
            }
            AnswerRecord answer = teachingRepository.findAnswer(request.answerId())
                    .orElseThrow(() -> new BusinessException("作答记录不存在"));
            if (request.score() != null) {
                if (request.score() < 0 || request.score() > 100) {
                    return AppResult.fail("分值必须在 0 到 100 之间");
                }
                teachingRepository.updateAnswerScore(request.answerId(), request.score());
            } else if (answer.score() == null) {
                return AppResult.fail("当前作答尚无得分，请先录入得分");
            }

            LocalDateTime now = TimeFormats.now();
            teachingRepository.saveTeacherFeedback(request.answerId(), ctx.user().accountId(), request.feedback().trim(), now);
            teachingRepository.updateAnswerStatus(request.answerId(), "已批改", request.feedback().trim(), ctx.user().accountId(), now);

            eventPublisher.publishEvent(new DomainNotificationEvent(
                    List.of(answer.studentId()),
                    "TEACHER_FEEDBACK",
                    "教师已批改并填写反馈",
                    "您的练习作答已收到教师反馈：" + request.feedback().trim(),
                    false
            ));
            return AppResult.ok(null);
        });
    }

    private String resolveTeacherClassId(String teacherId, String classId) {
        if (classId != null && !classId.isBlank()) {
            verifyOwnedClass(teacherId, classId);
            return classId;
        }
        PageResult<TeachingClass> classes = teachingAdminRepository.findClasses(null, teacherId, 1, 1);
        if (classes.records().isEmpty()) {
            throw new BusinessException("当前教师尚未关联班级");
        }
        return classes.records().get(0).classId();
    }

    private void verifyOwnedClass(String teacherId, String classId) {
        TeachingClass tc = teachingAdminRepository.findClass(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (!teacherId.equals(tc.teacherId())) {
            throw new BusinessException("无权访问非本人负责的班级");
        }
    }

    private String resolveUserName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }
}
