package com.seii.legal.data.repository;

import com.seii.legal.data.entity.ExportFile;
import com.seii.legal.data.entity.ExportRecord;

import java.util.List;

public interface ExportRepository {
    String saveRecord(ExportRecord record, String requestId);

    String saveFile(ExportFile file, String exportId);

    List<ExportRecord> findRecords(String taskId);
}
