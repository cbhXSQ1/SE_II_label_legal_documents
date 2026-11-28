package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.Draft;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.ReadingRecord;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.AnnotationDtos;
import com.seii.legal.rule.task.TaskRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 标注工作台功能层服务（ANNO-01…05）。
 */
@Service
@RequiredArgsConstructor
public class AnnotationAppService {

    private static final Set<IdentityType> ANNOTATOR_ONLY = Set.of(IdentityType.ANNOTATOR);
    private static final Set<IdentityType> READ_ROLES = Set.of(
            IdentityType.ANNOTATOR, IdentityType.ARBITER, IdentityType.TASK_MANAGER, IdentityType.ADMIN);

    private final AuthActionTemplate authTemplate;
    private final TaskRepository taskRepository;
    private final GuideRepository guideRepository;
    private final AnnotationRepository annotationRepository;
    private final AdjudicationRepository adjudicationRepository;
    private final TaskRule taskRule;
    private final AnnotationPayloadAdapter payloadAdapter;
    private final AllowedActionResolver actionResolver;

    @Transactional(readOnly = true)
    public AppResult<AnnotationDtos.TaskGuideResponse> taskGuide(String credential, String taskId) {
        return authTemplate.execute(credential, READ_ROLES, taskId, null, "无权查看任务绑定指南", ctx -> {
            AnnotationTask task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new BusinessException("任务不存在"));
            String version = guideRepository.findReference("TASK", taskId).orElse(task.guideVersion());
            GuideVersion guide = guideRepository.findVersion(version).orElse(null);
            boolean confirmed = guideRepository.findReadingRecord(ctx.user().accountId(), version)
                    .map(ReadingRecord::confirmed)
                    .orElse(false);
            return AppResult.ok(new AnnotationDtos.TaskGuideResponse(
                    version,
                    guide == null ? "全部任务" : guide.scope(),
                    confirmed
            ));
        });
    }

    @Transactional
    public AppResult<Void> confirmGuide(String credential, AnnotationDtos.ConfirmGuideRequest request) {
        return authTemplate.execute(credential, ANNOTATOR_ONLY, request.taskId(), null, "仅标注员需确认指南阅读", ctx -> {
            AnnotationTask task = requireAssignedAnnotatorTask(request.taskId(), ctx.user().accountId());
            String version = guideRepository.findReference("TASK", request.taskId()).orElse(task.guideVersion());
            LocalDateTime now = TimeFormats.now();
            guideRepository.saveReadingRecord(new ReadingRecord(
                    ctx.user().accountId(), version, now, request.confirmed() ? now : null, request.confirmed()));
            return AppResult.ok(null, List.of("进入标注工作台"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<AnnotationDtos.LoadAnnotationResponse> load(
            String credential, String taskId, String documentId, RoundType requestedRound) {
        return authTemplate.execute(credential, READ_ROLES, taskId, null, "无权加载该标注内容", ctx -> {
            AnnotationTask task = requireReadableTask(taskId, ctx.user().accountId(), ctx.user().identity());
            RoundType round = requestedRound != null ? requestedRound : inferRound(task.status());

            List<Submission> mySubmissions = annotationRepository.findSubmissions(
                    taskId, documentId, round, ctx.user().accountId());
            Optional<Submission> activeSub = mySubmissions.stream()
                    .filter(s -> s.status() == SubmissionStatus.SUBMITTED || s.status() == SubmissionStatus.LOCKED)
                    .findFirst();

            if (activeSub.isPresent()) {
                AnnotationPayloadAdapter.AnnotationContent content = payloadAdapter.fromStorageJson(activeSub.get().content());
                return AppResult.ok(
                        new AnnotationDtos.LoadAnnotationResponse(content, true, round),
                        actionResolver.resolve(task.status(), ctx.user().identity()));
            }

            Optional<Draft> draft = annotationRepository.findDraft(taskId, documentId, ctx.user().accountId());
            if (draft.isPresent()) {
                AnnotationPayloadAdapter.AnnotationContent content = payloadAdapter.fromStorageJson(draft.get().contentJson());
                return AppResult.ok(
                        new AnnotationDtos.LoadAnnotationResponse(content, false, round),
                        actionResolver.resolve(task.status(), ctx.user().identity()));
            }

            if (round == RoundType.STRUCTURED) {
                Optional<FinalVersion> unifiedCut = adjudicationRepository.findFinalVersion(
                        taskId, documentId, VersionType.CUTTING);
                if (unifiedCut.isPresent() && unifiedCut.get().contentJson() != null) {
                    return AppResult.ok(
                            new AnnotationDtos.LoadAnnotationResponse(
                                    payloadAdapter.fromStorageJson(unifiedCut.get().contentJson()), false, round),
                            actionResolver.resolve(task.status(), ctx.user().identity()));
                }
            }

            return AppResult.ok(
                    new AnnotationDtos.LoadAnnotationResponse(AnnotationPayloadAdapter.AnnotationContent.empty(), false, round),
                    actionResolver.resolve(task.status(), ctx.user().identity()));
        });
    }

    @Transactional
    public AppResult<AnnotationDtos.SaveDraftResponse> saveDraft(
            String credential, AnnotationDtos.SaveDraftRequest request) {
        return authTemplate.execute(credential, ANNOTATOR_ONLY, request.taskId(), null, "仅本任务标注员可保存草稿", ctx -> {
            AnnotationTask task = requireAssignedAnnotatorTask(request.taskId(), ctx.user().accountId());
            RoundType round = request.round() != null ? request.round() : inferRound(task.status());
            AnnotationPayloadAdapter.AnnotationContent normalized = payloadAdapter.normalize(
                    request.content(), request.flatContent());
            String contentJson = payloadAdapter.writeJson(normalized);
            LocalDateTime now = TimeFormats.now();

            Draft draft = new Draft(
                    request.taskId(),
                    request.documentId(),
                    ctx.user().accountId(),
                    round,
                    payloadAdapter.writeJson(normalized.pieces()),
                    payloadAdapter.writeJson(normalized.segments()),
                    payloadAdapter.writeJson(normalized.relations()),
                    payloadAdapter.writeJson(normalized.diagram()),
                    contentJson,
                    now
            );
            annotationRepository.saveDraft(draft);
            return AppResult.ok(new AnnotationDtos.SaveDraftResponse(TimeFormats.format(now)));
        });
    }

    @Transactional
    public AppResult<AnnotationDtos.SubmitAnnotationResponse> submit(
            String credential, AnnotationDtos.SubmitAnnotationRequest request) {
        return authTemplate.execute(credential, ANNOTATOR_ONLY, request.taskId(), null, "仅本任务标注员可提交标注", ctx -> {
            AnnotationTask task = requireAssignedAnnotatorTask(request.taskId(), ctx.user().accountId());
            LocalDateTime now = TimeFormats.now();
            if (task.deadline() != null && !taskRule.canSubmit(task.deadline(), now)) {
                return AppResult.fail("已超过任务截止时间，无法提交");
            }
            RoundType round = request.round() != null ? request.round() : inferRound(task.status());
            String contentJson = payloadAdapter.toStorageJson(request.content(), request.flatContent());
            String requestId = (request.requestId() != null && !request.requestId().isBlank())
                    ? request.requestId()
                    : UUID.randomUUID().toString();

            Submission submission = new Submission(
                    null,
                    request.taskId(),
                    request.documentId(),
                    round,
                    ctx.user().accountId(),
                    now,
                    task.guideVersion(),
                    contentJson,
                    SubmissionStatus.SUBMITTED,
                    null
            );
            String submissionId = annotationRepository.saveSubmission(submission, requestId);

            List<Submission> allRoundSubs = annotationRepository.findSubmissions(
                    request.taskId(), request.documentId(), round, null);
            taskRule.progress(request.taskId(), allRoundSubs);

            return AppResult.ok(
                    new AnnotationDtos.SubmitAnnotationResponse(submissionId, TimeFormats.format(now)),
                    List.of("查看最终版本"));
        });
    }

    private RoundType inferRound(TaskStatus status) {
        if (status == TaskStatus.CUTTING_CONFIRMED
                || status == TaskStatus.STRUCTURED_PENDING
                || status == TaskStatus.CONFIRMED) {
            return RoundType.STRUCTURED;
        }
        return RoundType.CUTTING;
    }

    private AnnotationTask requireAssignedAnnotatorTask(String taskId, String accountId) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (task.annotatorIds() == null || !task.annotatorIds().contains(accountId)) {
            throw new BusinessException("您未被分配该标注任务");
        }
        return task;
    }

    private AnnotationTask requireReadableTask(String taskId, String accountId, IdentityType identity) {
        AnnotationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (identity == IdentityType.ADMIN) {
            return task;
        }
        if (identity == IdentityType.ANNOTATOR && task.annotatorIds() != null && task.annotatorIds().contains(accountId)) {
            return task;
        }
        if (identity == IdentityType.ARBITER && accountId.equals(task.arbiterId())) {
            return task;
        }
        if (identity == IdentityType.TASK_MANAGER && accountId.equals(task.publisherId())) {
            return task;
        }
        throw new BusinessException("无权访问该任务");
    }
}
