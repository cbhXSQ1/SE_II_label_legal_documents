package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.common.enums.VersionType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.ExportFile;
import com.seii.legal.data.entity.ExportRecord;
import com.seii.legal.data.entity.FinalVersion;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.AdjudicationRepository;
import com.seii.legal.data.repository.ExportRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.data.repository.TaskRepository;
import com.seii.legal.function.dto.export.ExportCommand;
import com.seii.legal.function.dto.export.ExportResult;
import com.seii.legal.function.dto.export.ExportTaskListView;
import com.seii.legal.function.dto.export.ExportTaskView;
import com.seii.legal.function.dto.export.ImportCommand;
import com.seii.legal.function.dto.export.ImportResult;
import com.seii.legal.rule.export.ExportRule;
import com.seii.legal.rule.identity.IdentityRule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportAppService {

    private static final String EXPORT_FORBIDDEN = "只有任务管理者可以导出";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final IdentityRule identityRule;
    private final TaskRepository taskRepository;
    private final ExportRepository exportRepository;
    private final FileRepository fileRepository;
    private final AdjudicationRepository adjudicationRepository;
    private final ExportRule exportRule;

    public ExportAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            IdentityRule identityRule,
            TaskRepository taskRepository,
            ExportRepository exportRepository,
            FileRepository fileRepository,
            AdjudicationRepository adjudicationRepository,
            ExportRule exportRule) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.identityRule = identityRule;
        this.taskRepository = taskRepository;
        this.exportRepository = exportRepository;
        this.fileRepository = fileRepository;
        this.adjudicationRepository = adjudicationRepository;
        this.exportRule = exportRule;
    }

    @Transactional(readOnly = true)
    public AppResult<ExportTaskListView> exportableTasks(String credential) {
        AuthAttempt auth = authenticate(credential);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        String managerId = auth.user().accountId();
        PageResult<AnnotationTask> page = taskRepository.findByManager(managerId, 1, 1000);
        List<ExportTaskView> tasks = new ArrayList<>();
        if (page != null && page.records() != null) {
            for (AnnotationTask task : page.records()) {
                if (task.status() != TaskStatus.CONFIRMED || !managerId.equals(task.publisherId())) {
                    continue;
                }
                FinalVersion version = findFinalVersion(task).orElse(null);
                String confirmedAt = version == null || version.confirmedAt() == null
                        ? null : version.confirmedAt().format(TIME_FORMAT);
                int annotatorCount = task.annotatorIds() == null ? 0 : task.annotatorIds().size();
                tasks.add(new ExportTaskView(task.taskId(), task.name(), confirmedAt,
                        annotatorCount, task.guideVersion()));
            }
        }
        return AppResult.ok(new ExportTaskListView(tasks));
    }

    @Transactional
    public AppResult<ExportResult> create(String credential, ExportCommand command) {
        AuthAttempt auth = authenticate(credential);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        AnnotationTask task = taskRepository.findById(command.taskId()).orElse(null);
        if (task == null) {
            return AppResult.fail("任务不存在");
        }
        String managerId = auth.user().accountId();
        if (!managerId.equals(task.publisherId())) {
            return AppResult.fail("只能导出自己创建的任务");
        }
        if (task.status() != TaskStatus.CONFIRMED) {
            return AppResult.fail("请先确认最终版本");
        }
        FinalVersion version = findFinalVersion(task).orElse(null);
        if (version == null) {
            return AppResult.fail("最终版本不存在");
        }
        if (command.requestId() == null || command.requestId().isBlank()) {
            return AppResult.fail("请求标识不能为空");
        }
        List<String> existing = exportRepository.findRecords(command.taskId()).stream()
                .map(ExportRecord::fileName)
                .toList();
        if (exportRule.fileNameExists(command.fileName(), existing)) {
            return AppResult.fail("文件名已经存在");
        }
        exportRule.assemble(version, command.content());
        String payload = "Excel".equalsIgnoreCase(command.dataFormat())
                ? exportRule.toExcel(version)
                : exportRule.toJson(version);
        LocalDateTime now = LocalDateTime.now();
        ExportRecord record = new ExportRecord(
                null,
                managerId,
                now,
                command.taskId(),
                task.guideVersion(),
                command.fileName(),
                command.dataFormat());
        String exportId = exportRepository.saveRecord(record, command.requestId());
        ExportFile file = new ExportFile(
                null,
                exportId,
                command.fileName(),
                command.dataFormat(),
                payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8),
                now);
        String fileId = exportRepository.saveFile(file, exportId);
        return AppResult.ok(new ExportResult(exportId, "/app/export/files/" + fileId),
                List.of("下载导出文件", "查看导出历史"));
    }

    @Transactional
    public AppResult<ImportResult> importFile(String credential, ImportCommand command) {
        AuthAttempt auth = authenticate(credential);
        if (auth.error() != null) {
            return AppResult.fail(auth.error());
        }
        if (command.fileId() == null || command.fileId().isBlank()) {
            return AppResult.fail("文件不存在");
        }
        StoredFile file = fileRepository.find(command.fileId()).orElse(null);
        if (file == null) {
            return AppResult.fail("文件不存在");
        }
        ExportRule.ImportPreview preview = exportRule.parseImport(file.content(), null);
        if (preview == null) {
            return AppResult.fail("导入文件解析失败");
        }
        if (preview.errors() != null && !preview.errors().isEmpty()) {
            return AppResult.fail("导入失败：" + String.join("；", preview.errors()));
        }
        AnnotationTask draft = new AnnotationTask(
                null,
                SystemType.ANNOTATION,
                preview.taskName(),
                "由导出文件再导入",
                LocalDateTime.now().plusDays(7),
                null,
                List.of(),
                List.of(),
                null,
                TaskStatus.DRAFT,
                true,
                auth.user().accountId(),
                null,
                null);
        String taskId = taskRepository.save(draft);
        return AppResult.ok(new ImportResult(taskId, preview), List.of("修改任务", "发布任务"));
    }

    private AuthAttempt authenticate(String credential) {
        LoginUser user = sessionRepository.findByCredential(credential).orElse(null);
        if (user == null) {
            return new AuthAttempt(null, "登录已失效，请重新登录");
        }
        Account account = accountRepository.findById(user.accountId()).orElse(null);
        if (account == null) {
            return new AuthAttempt(null, "账号不存在");
        }
        if (!identityRule.isEnabled(account)) {
            return new AuthAttempt(null, "账号已停用，请联系管理员");
        }
        if (user.identity() != IdentityType.TASK_MANAGER
                || !identityRule.hasRole(account, IdentityType.TASK_MANAGER)) {
            return new AuthAttempt(null, EXPORT_FORBIDDEN);
        }
        return new AuthAttempt(user, null);
    }

    private Optional<FinalVersion> findFinalVersion(AnnotationTask task) {
        if (task.documentIds() == null || task.documentIds().isEmpty()) {
            return Optional.empty();
        }
        String documentId = task.documentIds().get(0);
        Optional<FinalVersion> version = adjudicationRepository.findFinalVersion(
                task.taskId(), documentId, VersionType.FINAL);
        if (version.isEmpty()) {
            version = adjudicationRepository.findFinalVersion(task.taskId(), documentId);
        }
        return version;
    }

    private record AuthAttempt(LoginUser user, String error) {
    }
}
