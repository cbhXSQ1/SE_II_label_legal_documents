package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.result.BusinessException;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.common.result.TimeFormats;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.Document;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.DocumentRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.function.dto.DocumentDtos;
import com.seii.legal.rule.task.TaskRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 文书与案件信息功能层服务（DOC-01…04）。
 */
@Service
@RequiredArgsConstructor
public class DocumentAppService {

    private static final Set<IdentityType> UPLOAD_ROLES = Set.of(IdentityType.TASK_MANAGER, IdentityType.TEACHER, IdentityType.ADMIN);

    private final AuthActionTemplate authTemplate;
    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final AccountRepository accountRepository;
    private final TaskRule taskRule;

    @Transactional
    public AppResult<DocumentDtos.UploadDocumentResponse> upload(
            String credential, DocumentDtos.UploadDocumentRequest request) {
        return authTemplate.execute(credential, UPLOAD_ROLES, null, null, "当前身份无权上传文书", ctx -> {
            byte[] fileBytes = null;
            String rawText = request.rawText();
            String title = request.title();

            if (request.fileId() != null && !request.fileId().isBlank()) {
                StoredFile file = fileRepository.find(request.fileId())
                        .orElseThrow(() -> new BusinessException("文书文件不存在"));
                fileBytes = file.content();
                if (title == null || title.isBlank()) {
                    title = file.fileName();
                }
                if ((rawText == null || rawText.isBlank()) && fileBytes != null) {
                    rawText = new String(fileBytes, StandardCharsets.UTF_8);
                }
            }
            if (rawText == null) {
                rawText = "";
            }
            String recognizedType = taskRule.recognizeDocumentType(request.type(), fileBytes);
            String docType = (recognizedType == null || recognizedType.isBlank()) ? request.type() : recognizedType;

            TaskRule.TextRange range = rawText.isBlank() ? new TaskRule.TextRange(0, 0) : taskRule.locateReasoning(rawText, docType);
            String reasonText = extractRange(rawText, range);
            String rangeText = range == null ? "0,0" : (range.start() + "," + range.end());

            CaseInfo parsedInfo = rawText.isBlank() ? null : taskRule.parseCaseInfo(rawText, docType);
            String effectiveCaseNo = firstNonBlank(request.caseNo(), parsedInfo != null ? parsedInfo.caseNo() : null);

            if (effectiveCaseNo != null && !effectiveCaseNo.isBlank()) {
                Optional<Document> existing = documentRepository.findByCaseNo(effectiveCaseNo);
                if (existing.isPresent()) {
                    Document dup = existing.get();
                    return AppResult.ok(new DocumentDtos.UploadDocumentResponse(
                            dup.documentId(),
                            dup.title(),
                            dup.caseNo(),
                            dup.type(),
                            dup.reasonText(),
                            documentRepository.findCaseInfo(dup.documentId()).map(this::toPayload).orElse(null),
                            true,
                            dup.documentId()
                    ), List.of("使用已有文书"));
                }
            }

            LocalDateTime now = TimeFormats.now();
            String safeTitle = firstNonBlank(title, effectiveCaseNo, "未命名文书");
            Document doc = new Document(
                    null,
                    safeTitle,
                    effectiveCaseNo,
                    docType,
                    rawText,
                    reasonText,
                    rangeText,
                    request.fileId(),
                    ctx.user().system(),
                    ctx.user().accountId(),
                    now
            );
            String docId = documentRepository.save(doc);

            CaseInfo savedCaseInfo = null;
            if (parsedInfo != null) {
                savedCaseInfo = new CaseInfo(
                        docId,
                        firstNonBlank(effectiveCaseNo, parsedInfo.caseNo()),
                        parsedInfo.court(),
                        parsedInfo.level(),
                        parsedInfo.trial(),
                        parsedInfo.date(),
                        parsedInfo.nature(),
                        parsedInfo.cause(),
                        parsedInfo.resultType(),
                        parsedInfo.caseType(),
                        parsedInfo.caseName(),
                        parsedInfo.publishDate(),
                        parsedInfo.storageNo(),
                        parsedInfo.relatedLaws(),
                        parsedInfo.judgePoints(),
                        parsedInfo.judgeGist(),
                        true,
                        false,
                        ctx.user().accountId(),
                        now
                );
                documentRepository.saveCaseInfo(docId, savedCaseInfo, true, false, ctx.user().accountId(), now);
            }

            return AppResult.ok(new DocumentDtos.UploadDocumentResponse(
                    docId,
                    safeTitle,
                    effectiveCaseNo,
                    docType,
                    reasonText,
                    savedCaseInfo == null ? null : toPayload(savedCaseInfo),
                    false,
                    null
            ), List.of("填写案件信息", "创建标注任务"));
        });
    }

    @Transactional(readOnly = true)
    public AppResult<DocumentDtos.DocumentDetailResponse> detail(String credential, String documentId) {
        return authTemplate.executeAuthenticated(credential, ctx -> {
            Document doc = documentRepository.findById(documentId)
                    .orElseThrow(() -> new BusinessException("文书不存在"));
            CaseInfo info = documentRepository.findCaseInfo(documentId).orElse(null);
            String filledBy = info == null ? null : resolveUserName(info.filledBy());
            String filledAt = info == null ? null : TimeFormats.format(info.filledAt());
            return AppResult.ok(new DocumentDtos.DocumentDetailResponse(
                    doc.documentId(),
                    doc.title(),
                    doc.caseNo(),
                    doc.type(),
                    doc.reasonText(),
                    info == null ? null : toPayload(info),
                    filledBy,
                    filledAt
            ));
        });
    }

    @Transactional
    public AppResult<Void> saveCaseInfo(String credential, String documentId, DocumentDtos.SaveCaseInfoRequest request) {
        return authTemplate.execute(credential, UPLOAD_ROLES, null, null, "当前身份无权修改案件信息", ctx -> {
            Document doc = documentRepository.findById(documentId)
                    .orElseThrow(() -> new BusinessException("文书不存在"));
            LocalDateTime now = TimeFormats.now();
            DocumentDtos.CaseInfoPayload p = request.caseInfo();
            CaseInfo entity = new CaseInfo(
                    documentId, p.caseNo(), p.court(), p.level(), p.trial(), p.date(), p.nature(), p.cause(),
                    p.resultType(), p.caseType(), p.caseName(), p.publishDate(), p.storageNo(),
                    p.relatedLaws(), p.judgePoints(), p.judgeGist(), false, true, ctx.user().accountId(), now
            );
            List<String> missing = taskRule.checkCaseInfo(doc.type(), entity);
            if (missing != null && !missing.isEmpty()) {
                return AppResult.fail("缺少必填案件信息：" + String.join("、", missing));
            }
            documentRepository.saveCaseInfo(documentId, entity, false, true, ctx.user().accountId(), now);
            return AppResult.ok(null);
        });
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<DocumentDtos.DocumentSummary>> list(
            String credential, String keyword, SystemType system, Integer page, Integer size) {
        return authTemplate.execute(credential, UPLOAD_ROLES, null, null, "当前身份无权查看文书列表", ctx -> {
            int p = PageResult.normalizePage(page);
            int s = PageResult.normalizeSize(size);
            SystemType effectiveSystem = system != null ? system : ctx.user().system();
            if (ctx.user().identity() == IdentityType.ADMIN && system == null) {
                effectiveSystem = null;
            }
            PageResult<Document> raw = documentRepository.search(keyword, effectiveSystem, p, s);
            List<DocumentDtos.DocumentSummary> items = raw.records().stream()
                    .map(d -> new DocumentDtos.DocumentSummary(d.documentId(), d.title(), d.caseNo(), d.type(), null))
                    .toList();
            return AppResult.ok(new PageResult<>(items, raw.total(), raw.page(), raw.size()));
        });
    }

    private String extractRange(String text, TaskRule.TextRange range) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (range == null || range.start() < 0 || range.end() <= range.start() || range.end() > text.length()) {
            return text;
        }
        return text.substring(range.start(), range.end());
    }

    private DocumentDtos.CaseInfoPayload toPayload(CaseInfo info) {
        return new DocumentDtos.CaseInfoPayload(
                info.caseNo(), info.court(), info.level(), info.trial(), info.date(), info.nature(), info.cause(),
                info.resultType(), info.caseType(), info.caseName(), info.publishDate(), info.storageNo(),
                info.relatedLaws(), info.judgePoints(), info.judgeGist()
        );
    }

    private String resolveUserName(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "";
        }
        return accountRepository.findById(accountId).map(Account::name).orElse(accountId);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
