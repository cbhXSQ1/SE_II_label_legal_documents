package com.seii.legal.function.dto.document;

public record UploadDocumentCommand(
        String fileId,
        String fileName,
        String format,
        String content,
        String type,
        String title,
        String caseNo
) {
    public UploadDocumentCommand(String fileId, String type) {
        this(fileId, null, null, null, type, null, null);
    }

    public UploadDocumentCommand(String fileName, String format, String content, String type) {
        this(null, fileName, format, content, type, null, null);
    }

    public UploadDocumentCommand(String fileId, String fileName, String format, String content, String type) {
        this(fileId, fileName, format, content, type, null, null);
    }
}
