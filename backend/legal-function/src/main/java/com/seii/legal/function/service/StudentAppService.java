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
import com.seii.legal.data.entity.DifferenceItem;
import com.seii.legal.data.entity.Exercise;
import com.seii.legal.data.entity.LearningMaterial;
import com.seii.legal.data.entity.LearningRecord;
import com.seii.legal.data.entity.LearningStat;
import com.seii.legal.data.entity.Question;
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
import com.seii.legal.function.dto.student.ExerciseView;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAppService {

    private static final String PUBLISHED = "已发布";
    private static final String SUBMITTED = "已提交";
    private static final String DRAFT = "DRAFT";
    private static final int PAGE_SIZE = Integer.MAX_VALUE;

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final TeachingAdminRepository teachingAdminRepository;
    private final TeachingRepository teachingRepository;
    private final IdentityRule identityRule;
    private final TeachingRule teachingRule;
    private final TaskRule taskRule;
    private final ObjectMapper objectMapper;

    public StudentAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            TeachingAdminRepository teachingAdminRepository,
            TeachingRepository teachingRepository,
            IdentityRule identityRule,
            TeachingRule teachingRule,
            TaskRule taskRule,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.teachingAdminRepository = teachingAdminRepository;
        this.teachingRepository = teachingRepository;
        this.identityRule = identityRule;
        this.teachingRule = teachingRule;
        this.taskRule = taskRule;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AppResult<StudentMaterialsView> materials(String credential) {
        return withClassStudent(credential, context -> {
            PageResult<LearningMaterial> page = teachingRepository.findMaterials(
                    context.classId(), PUBLISHED, 1, PAGE_SIZE);
            List<MaterialView> materials = records(page).stream()
                    .map(material -> toMaterialView(context, material))
                    .toList();
            return AppResult.ok(new StudentMaterialsView(materials));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentExercisesView> exercises(String credential) {
        return withClassStudent(credential, context -> {
            PageResult<Exercise> page = teachingRepository.findExercises(
                    context.classId(), PUBLISHED, 1, PAGE_SIZE);
            LocalDateTime current = LocalDateTime.now();
            List<ExerciseView> exercises = new ArrayList<>();
            for (Exercise exercise : records(page)) {
                List<AnswerRecord> answers = teachingRepository.findAnswers(
                        context.user().accountId(), exercise.exerciseId());
                long remainingSeconds = exercise.deadline() == null ? 0
                        : Math.max(0, Duration.between(current, exercise.deadline()).getSeconds());
                boolean overdue = exercise.deadline() != null && exercise.deadline().isBefore(current);
                exercises.add(new ExerciseView(
                        exercise.exerciseId(),
                        exercise.name(),
                        exercise.deadline(),
                        allowRedo(exercise),
                        exercise.status(),
                        teachingRule.completion(answers),
                        remainingSeconds,
                        overdue,
                        exercise.createdAt()));
            }
            return AppResult.ok(new StudentExercisesView(exercises));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentExerciseDetailView> exerciseDetail(String credential, String exerciseId) {
        return withClassStudent(credential, context -> {
            Exercise exercise = findOwnedExercise(context, exerciseId);
            if (exercise == null) {
                return AppResult.fail("练习任务不存在");
            }
            List<Question> questions = loadQuestions(exercise);
            if (questions == null) {
                return AppResult.fail("题目不存在");
            }
            return AppResult.ok(new StudentExerciseDetailView(
                    exercise.exerciseId(),
                    exercise.name(),
                    exercise.deadline(),
                    allowRedo(exercise),
                    exercise.status(),
                    questions));
        });
    }

    @Transactional
    public AppResult<AnswerDraftResult> saveDraft(String credential, AnswerDraftCommand command) {
        if (command == null || isBlank(command.exerciseId())) {
            return AppResult.fail("请求参数不能为空");
        }
        return withClassStudent(credential, context -> {
            Exercise exercise = findOwnedExercise(context, command.exerciseId());
            if (exercise == null) {
                return AppResult.fail("练习任务不存在");
            }
            LocalDateTime savedAt = LocalDateTime.now();
            AnswerRecord draft = new AnswerRecord(
                    null,
                    exercise.exerciseId(),
                    context.user().accountId(),
                    null,
                    toJson(command.answers()),
                    savedAt,
                    0,
                    DRAFT,
                    null,
                    null);
            teachingRepository.saveAnswerDraft(draft);
            return AppResult.ok(new AnswerDraftResult(savedAt));
        });
    }

    @Transactional
    public AppResult<SubmitAnswerResult> submit(String credential, AnswerSubmitCommand command) {
        if (command == null || isBlank(command.exerciseId())) {
            return AppResult.fail("请求参数不能为空");
        }
        if (isBlank(command.requestId())) {
            return AppResult.fail("请求标识不能为空");
        }
        return withClassStudent(credential, context -> {
            Exercise exercise = findOwnedExercise(context, command.exerciseId());
            if (exercise == null) {
                return AppResult.fail("练习任务不存在");
            }
            LocalDateTime submittedAt = LocalDateTime.now();
            if (!taskRule.canSubmit(exercise.deadline(), submittedAt)) {
                return AppResult.fail("练习任务已截止");
            }
            List<AnswerRecord> previousAnswers = teachingRepository.findAnswers(
                    context.user().accountId(), exercise.exerciseId());
            if (!previousAnswers.isEmpty() && !allowRedo(exercise)) {
                return AppResult.fail("该练习不允许重做");
            }
            List<Question> questions = loadQuestions(exercise);
            if (questions == null) {
                return AppResult.fail("题目不存在");
            }
            Map<String, String> answerContents = answerContents(command.answers());
            List<Double> scores = new ArrayList<>();
            List<DifferenceItem> differences = new ArrayList<>();
            for (Question question : questions) {
                String answerContent = answerContents.getOrDefault(question.questionId(), "");
                TeachingRule.Score score = teachingRule.score(
                        answerContent, standardAnswer(question), question.type());
                if (score == null) {
                    scores.add(0.0);
                    continue;
                }
                scores.add(score.total());
                if (score.differenceItems() != null) {
                    differences.addAll(score.differenceItems());
                }
            }
            double average = round1(teachingRule.average(scores));
            String systemFeedback = systemFeedback(differences);
            AnswerRecord answer = new AnswerRecord(
                    null,
                    exercise.exerciseId(),
                    context.user().accountId(),
                    null,
                    toJson(command.answers()),
                    submittedAt,
                    0,
                    SUBMITTED,
                    systemFeedback,
                    null);
            String answerId = teachingRepository.saveAnswer(answer, command.requestId());
            teachingRepository.updateAnswerScore(answerId, average);
            teachingRepository.saveDifference(answerId, differences);
            return AppResult.ok(new SubmitAnswerResult(
                    answerId, average, SUBMITTED, List.copyOf(differences), systemFeedback));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<StudentAnswersView> answers(String credential, String exerciseId) {
        return withAnswerStudent(credential, context -> {
            List<AnswerRecord> answers = new ArrayList<>(teachingRepository.findAnswers(
                    context.user().accountId(), exerciseId));
            answers.sort(Comparator.comparing(
                    AnswerRecord::submittedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            return AppResult.ok(new StudentAnswersView(List.copyOf(answers)));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<AnswerDetailView> answerDetail(String credential, String answerId) {
        return withAnswerStudent(credential, context -> {
            AnswerRecord answer = teachingRepository.findAnswer(answerId).orElse(null);
            if (answer == null) {
                return AppResult.fail("作答记录不存在");
            }
            if (!context.user().accountId().equals(answer.studentId())) {
                return AppResult.fail("无权查看该作答记录");
            }
            List<DifferenceItem> differences = teachingRepository.findDifferences(answerId, null);
            if (differences == null) {
                differences = List.of();
            }
            if (differences.isEmpty() && answer.score() == 0 && SUBMITTED.equals(answer.status())) {
                return AppResult.fail("正在比对，请稍后刷新");
            }
            TeacherFeedback teacherFeedback = teachingRepository.findTeacherFeedback(answerId).orElse(null);
            AnswerDetailView view = new AnswerDetailView(
                    answer,
                    List.copyOf(differences),
                    systemFeedback(answer, differences),
                    teacherFeedback);
            if (teacherFeedback == null) {
                return new AppResult<>(true, "教师还没有填写反馈", view, List.of("等待教师反馈"));
            }
            return AppResult.ok(view);
        });
    }

    @Transactional(readOnly = true)
    public AppResult<LearningView> learning(
            String credential,
            String pointId,
            String exerciseId,
            LocalDateTime from,
            LocalDateTime to) {
        return withAnswerStudent(credential, context -> {
            List<MaterialView> materials = new ArrayList<>();
            if (context.classId() != null) {
                PageResult<LearningMaterial> page = teachingRepository.findMaterials(
                        context.classId(), PUBLISHED, 1, PAGE_SIZE);
                for (LearningMaterial material : records(page)) {
                    if (pointId == null || (material.pointIds() != null
                            && material.pointIds().contains(pointId))) {
                        materials.add(toMaterialView(context, material));
                    }
                }
            }
            List<AnswerRecord> answers = new ArrayList<>();
            for (AnswerRecord answer : teachingRepository.findAnswers(
                    context.user().accountId(), exerciseId)) {
                LocalDateTime submittedAt = answer.submittedAt();
                if (submittedAt == null) {
                    continue;
                }
                if (from != null && submittedAt.isBefore(from)) {
                    continue;
                }
                if (to != null && submittedAt.isAfter(to)) {
                    continue;
                }
                answers.add(answer);
            }
            answers.sort(Comparator.comparing(
                    AnswerRecord::submittedAt, Comparator.nullsLast(Comparator.reverseOrder())));
            LearningStat stat = teachingRepository.findLearningStat(
                    context.user().accountId(), context.classId(), pointId, exerciseId, from, to);
            List<LearningStat> stats = stat == null ? List.of() : List.of(stat);
            return AppResult.ok(new LearningView(List.copyOf(materials), List.copyOf(answers), stats));
        });
    }

    private <T> AppResult<T> withClassStudent(
            String credential,
            Function<StudentContext, AppResult<T>> action) {
        return withAuthenticatedStudent(credential, (user, account) -> {
            TeachingClass teachingClass = teachingAdminRepository
                    .findClassByStudentId(user.accountId())
                    .orElse(null);
            if (teachingClass == null) {
                return AppResult.fail("学生尚未加入班级");
            }
            if (identityRule.resolveScope(user, null, teachingClass.classId()) != DataScope.OWN_CLASS) {
                return AppResult.fail("当前身份不能查看该数据");
            }
            return action.apply(new StudentContext(user, account, teachingClass));
        });
    }

    private <T> AppResult<T> withAnswerStudent(
            String credential,
            Function<StudentContext, AppResult<T>> action) {
        return withAuthenticatedStudent(credential, (user, account) -> {
            if (identityRule.resolveScope(user, null, null) != DataScope.OWN_ANSWERS) {
                return AppResult.fail("当前身份不能查看该数据");
            }
            TeachingClass teachingClass = teachingAdminRepository
                    .findClassByStudentId(user.accountId())
                    .orElse(null);
            return action.apply(new StudentContext(user, account, teachingClass));
        });
    }

    private <T> AppResult<T> withAuthenticatedStudent(
            String credential,
            BiFunction<LoginUser, Account, AppResult<T>> action) {
        Optional<LoginUser> session = sessionRepository.findByCredential(credential);
        if (session.isEmpty()) {
            return AppResult.fail("登录已失效，请重新登录");
        }
        LoginUser user = session.get();
        Optional<Account> found = accountRepository.findById(user.accountId());
        if (found.isEmpty()) {
            return AppResult.fail("账号不存在");
        }
        Account account = found.get();
        if (!identityRule.isEnabled(account)) {
            return AppResult.fail("账号已停用，请联系管理员");
        }
        if (!IdentityType.STUDENT.equals(user.identity())
                || !identityRule.hasRole(account, IdentityType.STUDENT)) {
            return AppResult.fail("只有学生可以访问");
        }
        return action.apply(user, account);
    }

    private Exercise findOwnedExercise(StudentContext context, String exerciseId) {
        if (isBlank(exerciseId)) {
            return null;
        }
        Exercise exercise = teachingRepository.findExercise(exerciseId).orElse(null);
        if (exercise == null || context.classId() == null
                || !context.classId().equals(exercise.classId())) {
            return null;
        }
        return exercise;
    }

    private List<Question> loadQuestions(Exercise exercise) {
        PageResult<Question> page = teachingRepository.findQuestions(
                exercise.classId(), null, 1, PAGE_SIZE);
        Map<String, Question> byId = new HashMap<>();
        for (Question question : records(page)) {
            byId.put(question.questionId(), question);
        }
        List<String> questionIds = exercise.questionIds() == null ? List.of() : exercise.questionIds();
        List<Question> questions = new ArrayList<>();
        for (String questionId : questionIds) {
            Question question = byId.get(questionId);
            if (question == null) {
                return null;
            }
            questions.add(question);
        }
        return questions;
    }

    private MaterialView toMaterialView(StudentContext context, LearningMaterial material) {
        LearningRecord record = teachingRepository
                .findLearningRecord(context.user().accountId(), material.materialId())
                .orElse(null);
        boolean studied = record != null;
        return new MaterialView(
                material.materialId(),
                material.title(),
                null,
                material.content(),
                null,
                material.pointIds() == null ? List.of() : material.pointIds(),
                material.fileId(),
                material.classId(),
                context.teachingClass() == null ? null : context.teachingClass().name(),
                material.status(),
                material.createdAt(),
                studied,
                studied ? "已学习" : "未学习",
                record == null ? 0 : record.durationSeconds(),
                record == null ? null : record.readAt());
    }

    private Map<String, String> answerContents(List<AnswerDraftItem> answers) {
        Map<String, String> contents = new HashMap<>();
        if (answers == null) {
            return contents;
        }
        for (AnswerDraftItem answer : answers) {
            if (answer != null && answer.questionId() != null) {
                contents.put(answer.questionId(), answer.content() == null ? "" : answer.content());
            }
        }
        return contents;
    }

    private String standardAnswer(Question question) {
        if (question.standardJson() != null && !question.standardJson().isBlank()) {
            return question.standardJson();
        }
        return question.answer() == null ? "" : question.answer();
    }

    private String toJson(List<AnswerDraftItem> answers) {
        try {
            return objectMapper.writeValueAsString(answers == null ? List.of() : answers);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("作答内容保存失败");
        }
    }

    private String systemFeedback(AnswerRecord answer, List<DifferenceItem> differences) {
        if (answer.feedback() != null && !answer.feedback().isBlank()) {
            return answer.feedback();
        }
        return systemFeedback(differences);
    }

    private String systemFeedback(List<DifferenceItem> differences) {
        if (differences == null || differences.isEmpty()) {
            return "作答全部正确";
        }
        return "自动比对发现 " + differences.size() + " 项差异";
    }

    private boolean allowRedo(Exercise exercise) {
        String status = exercise.status();
        return status == null
                || (!"已截止".equals(status) && !"已结束".equals(status) && !"已下架".equals(status));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> List<T> records(PageResult<T> page) {
        if (page == null || page.records() == null) {
            return List.of();
        }
        return page.records();
    }

    private record StudentContext(LoginUser user, Account account, TeachingClass teachingClass) {
        String classId() {
            return teachingClass == null ? null : teachingClass.classId();
        }
    }
}
