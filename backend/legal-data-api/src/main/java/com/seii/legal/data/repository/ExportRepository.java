package com.seii.legal.data.repository;

import com.seii.legal.data.entity.ExportFile;
import com.seii.legal.data.entity.ExportRecord;
import com.seii.legal.data.entity.ImportPreviewRecord;
import java.util.List;
import java.util.Optional;

public interface ExportRepository {
    String saveRecord(ExportRecord record, String requestId);

    String saveFile(ExportFile file, String exportId);

    List<ExportRecord> findRecords(String taskId);

    String saveImportPreview(ImportPreviewRecord preview);

    Optional<ImportPreviewRecord> findImportPreview(String taskId);

    void deleteImportPreview(String taskId);
}
