package com.seii.legal.function.dto;

import com.seii.legal.rule.export.ExportRule;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class ExportDtos {

    private ExportDtos() {
    }

    public record ExportableTaskItem(
            String id,
            String name,
            String confirmedAt,
            int annotatorCount,
            String guideVersion
    ) {
    }

    public record ExportableTasksResponse(List<ExportableTaskItem> tasks) {
    }

    public record CreateExportRequest(
            @NotBlank(message = "任务标识不能为空") String taskId,
            @NotBlank(message = "请选择导出内容") String content,
            String dataFormat,
            String diagramFormat,
            @NotBlank(message = "请填写文件名") String fileName,
            String requestId
    ) {
    }

    public record CreateExportResponse(
            String exportId,
            String downloadUrl
    ) {
    }

    public record ImportExportRequest(
            @NotBlank(message = "请先上传导出压缩包") String fileId
    ) {
    }

    public record ImportExportResponse(
            String taskId,
            ExportRule.ImportPreview preview
    ) {
    }
}
