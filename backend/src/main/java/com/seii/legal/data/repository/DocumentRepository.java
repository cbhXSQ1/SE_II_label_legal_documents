package com.seii.legal.data.repository;

import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.Document;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DocumentRepository {
    String save(Document document);

    Optional<Document> findById(String documentId);

    Optional<Document> findByCaseNo(String caseNo);

    PageResult<Document> search(String keyword, SystemType system, int page, int size);

    void saveCaseInfo(String documentId, CaseInfo caseInfo, boolean autoParsed, boolean manualEdited, String operatorId, LocalDateTime time);

    Optional<CaseInfo> findCaseInfo(String documentId);

    void saveRangeChange(String documentId, String beforeRange, String afterRange, String operatorId, LocalDateTime time);
}
