package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.AnswerRecord;
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.LearningRecord;
import com.seii.legal.data.entity.LearningStat;
import com.seii.legal.data.entity.Question;
import com.seii.legal.data.entity.QuestionVersion;
import com.seii.legal.data.entity.TeacherFeedback;
import com.seii.legal.data.entity.TeachingClass;
import com.seii.legal.data.repository.TeachingAdminRepository;
import com.seii.legal.data.repository.TeachingRepository;
import com.seii.legal.function.dto.StudentDtos;
import com.seii.legal.rule.task.TaskRule;
import com.seii.legal.rule.teaching.TeachingRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 学生学习与作答功能层服务（STU-01…08）。
 */
@Service
@RequiredArgsConstructor
public class StudentAppService {

    private static final Set<IdentityType> STUDENT_ONLY = Set.of(IdentityType.STUDENT);

    private final AuthActionTemplate authTemplate;
    private final TeachingRepository teachingRepository;
    private final TeachingAdminRepository teachingAdminRepository;
    private final TeachingRule teachingRule;
    private final TaskRule taskRule;
    private final AnnotationPayloadAdapter jsonHelper;

    @Transactional(readOnly = true)
    public AppResult<StudentDtos.StudentMaterialsResponse> materials(String credential) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可查看学习材料", ctx -> {
            String classId = requireStudentClassId(ctx.user().accountId());
            List<LearningMaterial> list = teachingRepository.findMaterials(classId, "已发布", 1, 200).records();
            List<StudentDtos.StudentMaterialItem> items = list.stream()
                    .map(m -> {
                        Optional<LearningRecord> rec = teachingRepository.findLearningRecord(
                                ctx.user().accountId(), m.materialId());
                        return new StudentDtos.StudentMaterialItem(
                                m,
                                rec.map(LearningRecord::completionStatus).orElse("未开始"),
                                rec.map(LearningRecord::durationSeconds).orElse(0L),
                                rec.map(LearningRecord::confirmed).orElse(false)
                        );
                    })
                    .toList();
            return AppResult.ok(new StudentDtos.StudentMaterialsResponse(items));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentDtos.StudentExercisesResponse> exercises(String credential) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可查看练习任务", ctx -> {
            String classId = requireStudentClassId(ctx.user().accountId());
            LocalDateTime now = TimeFormats.now();
            List<Exercise> list = teachingRepository.findExercises(classId, "已发布", 1, 200).records();
            List<StudentDtos.StudentExerciseItem> items = list.stream()
                    .map(ex -> {
                        List<AnswerRecord> myAnswers = teachingRepository.findAnswers(ctx.user().accountId(), ex.exerciseId());
                        String completion = teachingRule.completion(myAnswers);
                        Long remaining = ex.deadline() == null
                                ? null
                                : Math.max(0L, Duration.between(now, ex.deadline()).getSeconds());
                        return new StudentDtos.StudentExerciseItem(ex, completion, remaining);
                    })
                    .toList();
            return AppResult.ok(new StudentDtos.StudentExercisesResponse(items));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentDtos.ExerciseDetailResponse> exerciseDetail(String credential, String exerciseId) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可查看练习题目", ctx -> {
            String classId = requireStudentClassId(ctx.user().accountId());
            Exercise ex = teachingRepository.findExercise(exerciseId)
                    .orElseThrow(() -> new BusinessException("练习任务不存在"));
            if (!classId.equals(ex.classId())) {
                return AppResult.fail("无权访问非本班练习任务");
            }
            List<Question> questions = new ArrayList<>();
            if (ex.questionIds() != null) {
                for (String qid : ex.questionIds()) {
                    teachingRepository.findQuestion(qid).ifPresent(questions::add);
                }
            }
            return AppResult.ok(new StudentDtos.ExerciseDetailResponse(
                    ex.exerciseId(), ex.name(), TimeFormats.format(ex.deadline()), ex.allowRedo(), questions));
        });
    }

    @Transactional
    public AppResult<StudentDtos.SaveAnswerDraftResponse> saveAnswerDraft(
            String credential, StudentDtos.SaveAnswerDraftRequest request) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可保存作答草稿", ctx -> {
            String classId = requireStudentClassId(ctx.user().accountId());
            Exercise ex = teachingRepository.findExercise(request.exerciseId())
                    .orElseThrow(() -> new BusinessException("练习任务不存在"));
            if (!classId.equals(ex.classId())) {
                return AppResult.fail("无权保存非本班练习草稿");
            }
            LocalDateTime now = TimeFormats.now();
            AnswerRecord draft = new AnswerRecord(
                    null,
                    ctx.user().accountId(),
                    request.exerciseId(),
                    "{}",
                    jsonHelper.writeJson(request.answers()),
                    null,
                    null,
                    0L,
                    null,
                    "草稿",
                    null
            );
            teachingRepository.saveAnswer(draft, "draft:" + ctx.user().accountId() + ":" + request.exerciseId());
            return AppResult.ok(new StudentDtos.SaveAnswerDraftResponse(TimeFormats.format(now)));
        });
    }

    @Transactional
    public AppResult<StudentDtos.SubmitAnswerResponse> submitAnswer(
            String credential, StudentDtos.SubmitAnswerRequest request) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可提交作答", ctx -> {
            String classId = requireStudentClassId(ctx.user().accountId());
            Exercise ex = teachingRepository.findExercise(request.exerciseId())
                    .orElseThrow(() -> new BusinessException("练习任务不存在"));
            if (!classId.equals(ex.classId())) {
                return AppResult.fail("无权提交非本班练习");
            }
            LocalDateTime now = TimeFormats.now();
            if (ex.deadline() != null && !taskRule.canSubmit(ex.deadline(), now)) {
                return AppResult.fail("练习任务已截止，无法提交");
            }
            List<AnswerRecord> prior = teachingRepository.findAnswers(ctx.user().accountId(), request.exerciseId());
            boolean hasSubmitted = prior.stream().anyMatch(a -> !"草稿".equals(a.status()));
            if (hasSubmitted && !ex.allowRedo()) {
                return AppResult.fail("该练习不允许重复作答");
            }

            Map<String, String> answerByQuestion = new LinkedHashMap<>();
            if (request.answers() != null) {
                for (StudentDtos.QuestionAnswerItem item : request.answers()) {
                    answerByQuestion.put(item.questionId(), item.content());
                }
            }

            List<String> questionIds = ex.questionIds() == null ? List.of() : ex.questionIds();
            Map<String, Integer> versionSnapshot = new LinkedHashMap<>();
            List<Double> perQuestionScores = new ArrayList<>();
            List<DifferenceItem> diffEntities = new ArrayList<>();

            String reqId = (request.requestId() != null && !request.requestId().isBlank())
                    ? request.requestId()
                    : UUID.randomUUID().toString();

            for (String qid : questionIds) {
                Question q = teachingRepository.findQuestion(qid).orElse(null);
                if (q == null) {
                    perQuestionScores.add(0.0);
                    continue;
                }
                versionSnapshot.put(qid, q.version());
                String standardAnswer = teachingRepository.findQuestionVersion(qid, q.version())
                        .map(QuestionVersion::standardAnswer)
                        .orElse(q.standardJson() != null ? q.standardJson() : q.answer());
                String studentContent = answerByQuestion.getOrDefault(qid, "");
                if (studentContent == null || studentContent.isBlank()) {
                    perQuestionScores.add(0.0);
                    diffEntities.add(new DifferenceItem(null, qid, "未作答", qid, standardAnswer, "", "未作答按 0 分计"));
                } else {
                    TeachingRule.Score scored = teachingRule.score(studentContent, standardAnswer, q.type());
                    perQuestionScores.add(scored.total());
                    if (scored.differenceItems() != null) {
                        for (String note : scored.differenceItems()) {
                            diffEntities.add(new DifferenceItem(null, qid, q.type(), qid, standardAnswer, studentContent, note));
                        }
                    }
                }
            }

            double finalScore = Math.round(teachingRule.average(perQuestionScores) * 10.0) / 10.0;
            String systemFeedback = diffEntities.isEmpty()
                    ? "全部题目与标准结果一致，表现优秀。"
                    : "自动评分完成，共发现 " + diffEntities.size() + " 处差异，请对照差异明细复盘。";

            AnswerRecord record = new AnswerRecord(
                    null,
                    ctx.user().accountId(),
                    request.exerciseId(),
                    jsonHelper.writeJson(versionSnapshot),
                    jsonHelper.writeJson(request.answers()),
                    now,
                    now,
                    0L,
                    finalScore,
                    "已提交",
                    systemFeedback
            );
            String answerId = teachingRepository.saveAnswer(record, reqId);
            teachingRepository.updateAnswerScore(answerId, finalScore);

            List<DifferenceItem> boundDiffs = diffEntities.stream()
                    .map(d -> new DifferenceItem(answerId, d.questionId(), d.dimension(), d.object(),
                            d.standardValue(), d.studentValue(), d.note()))
                    .toList();
            if (!boundDiffs.isEmpty()) {
                teachingRepository.saveDifference(answerId, boundDiffs);
            }

            return AppResult.ok(
                    new StudentDtos.SubmitAnswerResponse(answerId, finalScore, "已提交", boundDiffs, systemFeedback),
                    List.of("查看作答差异"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentDtos.AnswerListResponse> listAnswers(String credential, String exerciseId) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可查看本人作答记录", ctx -> {
            List<AnswerRecord> answers = teachingRepository.findAnswers(ctx.user().accountId(), exerciseId);
            return AppResult.ok(new StudentDtos.AnswerListResponse(answers));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentDtos.AnswerDetailResponse> answerDetail(String credential, String answerId) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可查看本人作答详情", ctx -> {
            AnswerRecord answer = teachingRepository.findAnswer(answerId)
                    .orElseThrow(() -> new BusinessException("作答记录不存在"));
            if (!ctx.user().accountId().equals(answer.studentId())) {
                return AppResult.fail("无权查看其他学生的作答记录");
            }
            List<DifferenceItem> diffs = teachingRepository.findDifferences(answerId, null);
            TeacherFeedback fb = teachingRepository.findTeacherFeedback(answerId).orElse(null);
            String sysFb = (answer.systemFeedback() == null || answer.systemFeedback().isBlank())
                    ? "正在比对，请稍后刷新"
                    : answer.systemFeedback();
            return AppResult.ok(new StudentDtos.AnswerDetailResponse(answer, diffs, sysFb, fb));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentDtos.StudentLearningResponse> learning(
            String credential, String pointId, String exerciseId, String from, String to) {
        return authTemplate.execute(credential, STUDENT_ONLY, null, null, "仅学生可查看本人学习情况", ctx -> {
            String classId = requireStudentClassId(ctx.user().accountId());
            LocalDateTime fromTime = TimeFormats.parse(from);
            LocalDateTime toTime = TimeFormats.parse(to);

            List<StudentDtos.StudentMaterialItem> materialItems = teachingRepository.findMaterials(classId, "已发布", 1, 200)
                    .records().stream()
                    .map(m -> {
                        Optional<LearningRecord> rec = teachingRepository.findLearningRecord(ctx.user().accountId(), m.materialId());
                        return new StudentDtos.StudentMaterialItem(
                                m,
                                rec.map(LearningRecord::completionStatus).orElse("未开始"),
                                rec.map(LearningRecord::durationSeconds).orElse(0L),
                                rec.map(LearningRecord::confirmed).orElse(false)
                        );
                    })
                    .toList();
            List<AnswerRecord> answers = teachingRepository.findAnswers(ctx.user().accountId(), exerciseId);
            LearningStat stat = teachingRepository.findLearningStat(
                    ctx.user().accountId(), classId, pointId, exerciseId, fromTime, toTime);
            List<LearningStat> stats = stat == null ? List.of() : List.of(stat);
            return AppResult.ok(new StudentDtos.StudentLearningResponse(materialItems, answers, stats));
        });
    }

    private String requireStudentClassId(String studentId) {
        TeachingClass tc = teachingAdminRepository.findClassByStudentId(studentId)
                .orElseThrow(() -> new BusinessException("当前学生尚未加入任何教学班级"));
        return tc.classId();
    }
}
