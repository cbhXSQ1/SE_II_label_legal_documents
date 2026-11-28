package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.ExportFile;
import com.seii.legal.data.entity.ExportRecord;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.ExportRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.GuideRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.ExportDtos;
import com.seii.legal.rule.export.ExportRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 导出与再导入功能层服务（EXP-01…03）。
 */
@Service
@RequiredArgsConstructor
public class ExportAppService {

    private static final Set<IdentityType> EXPORT_ROLES = Set.of(IdentityType.TASK_MANAGER, IdentityType.ADMIN);

    private final AuthActionTemplate authTemplate;
    private final TaskRepository taskRepository;
    private final AdjudicationRepository adjudicationRepository;
    private final ExportRepository exportRepository;
    private final FileRepository fileRepository;
    private final GuideRepository guideRepository;
    private final ExportRule exportRule;
    private final TaskDocumentAdapter documentAdapter;

    @Transactional(readOnly = true)
    public AppResult<ExportDtos.ExportableTasksResponse> listExportableTasks(String credential) {
        return authTemplate.execute(credential, EXPORT_ROLES, null, null, "仅任务管理者可导出结果", ctx -> {
            PageResult<AnnotationTask> raw = (ctx.user().identity() == IdentityType.ADMIN)
                    ? taskRepository.findAll(SystemType.ANNOTATION, 1, 200)
                    : taskRepository.findByManager(ctx.user().accountId(), 1, 200);
            List<ExportDtos.ExportableTaskItem> items = raw.records().stream()
                    .filter(t -> t.status() == TaskStatus.CONFIRMED)
                    .map(t -> {
                        String docId = documentAdapter.primaryDocumentId(t.documentIds());
                        Optional<FinalVersion> fv = docId == null
                                ? Optional.empty()
                                : adjudicationRepository.findFinalVersion(t.taskId(), docId, VersionType.FINAL);
                        String confirmedAt = fv.map(v -> TimeFormats.format(v.confirmedAt())).orElse("");
                        int count = t.annotatorIds() == null ? 0 : t.annotatorIds().size();
                        return new ExportDtos.ExportableTaskItem(t.taskId(), t.name(), confirmedAt, count, t.guideVersion());
                    })
                    .toList();
            return AppResult.ok(new ExportDtos.ExportableTasksResponse(items));
        });
    }

    @Transactional
    public AppResult<ExportDtos.CreateExportResponse> createExport(
            String credential, ExportDtos.CreateExportRequest request) {
        return authTemplate.execute(credential, EXPORT_ROLES, request.taskId(), null, "仅任务管理者可导出结果", ctx -> {
            AnnotationTask task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new BusinessException("任务不存在"));
            if (ctx.user().identity() == IdentityType.TASK_MANAGER && !ctx.user().accountId().equals(task.publisherId())) {
                return AppResult.fail("仅任务创建者可导出该任务");
            }
            if (task.status() != TaskStatus.CONFIRMED) {
                return AppResult.fail("请先确认最终版本");
            }

            List<String> existingNames = exportRepository.findRecords(request.taskId()).stream()
                    .map(ExportRecord::fileName)
                    .toList();
            if (exportRule.fileNameExists(request.fileName(), existingNames)) {
                return AppResult.fail("文件名已经存在");
            }

            String docId = documentAdapter.primaryDocumentId(task.documentIds());
            FinalVersion finalVersion = adjudicationRepository.findFinalVersion(request.taskId(), docId, VersionType.FINAL)
                    .orElseThrow(() -> new BusinessException("请先确认最终版本"));

            String assembled = exportRule.assemble(finalVersion, request.content());
            if ("JSON".equalsIgnoreCase(request.dataFormat())) {
                exportRule.toJson(finalVersion);
            } else if ("Excel".equalsIgnoreCase(request.dataFormat())) {
                exportRule.toExcel(finalVersion);
            }

            LocalDateTime now = TimeFormats.now();
            byte[] payload = (assembled == null ? "" : assembled).getBytes(StandardCharsets.UTF_8);
            String fileId = fileRepository.save(request.fileName() + ".zip", "zip", payload, ctx.user().accountId(), now, "EXPORT");
            String requestId = (request.requestId() != null && !request.requestId().isBlank())
                    ? request.requestId()
                    : UUID.randomUUID().toString();

            String exportId = exportRepository.saveRecord(new ExportRecord(
                    null,
                    ctx.user().accountId(),
                    now,
                    request.taskId(),
                    task.guideVersion(),
                    request.content(),
                    request.fileName(),
                    request.dataFormat(),
                    request.diagramFormat(),
                    fileId
            ), requestId);
            exportRepository.saveFile(new ExportFile(fileId, exportId, request.fileName() + ".zip", "zip", payload), exportId);

            return AppResult.ok(new ExportDtos.CreateExportResponse(exportId, "/app/file/" + fileId), List.of("下载压缩包"));
        });
    }

    @Transactional
    public AppResult<ExportDtos.ImportExportResponse> importExport(
            String credential, ExportDtos.ImportExportRequest request) {
        return authTemplate.execute(credential, EXPORT_ROLES, null, null, "仅任务管理者可执行再导入", ctx -> {
            StoredFile stored = fileRepository.find(request.fileId())
                    .orElseThrow(() -> new BusinessException("导入文件不存在"));
            List<GuideVersion> guides = guideRepository.findAvailable();
            String currentGuide = guides.isEmpty() ? "" : guides.get(0).version();

            ExportRule.ImportPreview preview = exportRule.parseImport(stored.content(), currentGuide);
            if (preview == null) {
                return AppResult.fail("导入压缩包解析失败");
            }
            if (preview.errors() != null && !preview.errors().isEmpty()) {
                return AppResult.fail("再导入校验失败：" + String.join("；", preview.errors()));
            }

            AnnotationTask draft = new AnnotationTask(
                    null,
                    SystemType.ANNOTATION,
                    preview.taskName() == null || preview.taskName().isBlank() ? "再导入任务草稿" : preview.taskName(),
                    "由导出压缩包再导入生成",
                    null,
                    currentGuide,
                    List.of(),
                    List.of(),
                    null,
                    TaskStatus.DRAFT,
                    true,
                    ctx.user().accountId(),
                    null,
                    null
            );
            String newTaskId = taskRepository.save(draft);
            return AppResult.ok(new ExportDtos.ImportExportResponse(newTaskId, preview), List.of("编辑新任务草稿"));
        });
    }
}
