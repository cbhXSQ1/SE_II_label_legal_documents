package com.seii.legal.rule.export;

import com.seii.legal.data.entity.FinalVersion;

import java.util.List;

public interface ExportRule {
    String assemble(FinalVersion version, String selection);

    String toJson(FinalVersion version);

    String toExcel(FinalVersion version);

    boolean fileNameExists(String fileName, List<String> existing);

    ImportPreview parseImport(byte[] file, String currentGuideVersion);

    record ImportPreview(
            String taskName,
            int classCount,
            int studentCount,
            boolean guideVersionMatch,
            List<String> errors
    ) {
    }
}
