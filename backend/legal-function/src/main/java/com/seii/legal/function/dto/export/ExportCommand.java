package com.seii.legal.function.dto.export;

public record ExportCommand(
        String taskId,
        String content,
        String dataFormat,
        String diagramFormat,
        String fileName,
        String requestId
) {
}
