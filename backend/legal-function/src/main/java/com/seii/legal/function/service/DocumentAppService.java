package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.LoginUser;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.data.entity.Account;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.Document;
import com.seii.legal.data.entity.StoredFile;
import com.seii.legal.data.repository.AccountRepository;
import com.seii.legal.data.repository.DocumentRepository;
import com.seii.legal.data.repository.FileRepository;
import com.seii.legal.data.repository.SessionRepository;
import com.seii.legal.function.dto.document.CaseInfoView;
import com.seii.legal.function.dto.document.DocumentDetailView;
import com.seii.legal.function.dto.document.DocumentListItemView;
import com.seii.legal.function.dto.document.DocumentUploadResult;
import com.seii.legal.function.dto.document.SaveCaseInfoCommand;
import com.seii.legal.function.dto.document.UploadDocumentCommand;
import com.seii.legal.rule.identity.IdentityRule;
import com.seii.legal.rule.task.TaskRule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentAppService {

    private static final Set<IdentityType> MANAGE_IDENTITIES = Set.of(
            IdentityType.TASK_MANAGER, IdentityType.TEACHER);
    private static final Set<IdentityType> VIEW_IDENTITIES = Set.of(
            IdentityType.TASK_MANAGER,
            IdentityType.TEACHER,
            IdentityType.ANNOTATOR,
            IdentityType.ARBITER,
            IdentityType.ADMIN);
    private static final String MANAGE_FORBIDDEN = "只有任务管理者或教师可以管理文书";
    private static final String VIEW_FORBIDDEN = "当前身份不能查看文书";

    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final TaskRule taskRule;
    private final IdentityRule identityRule;

    public DocumentAppService(
            SessionRepository sessionRepository,
            AccountRepository accountRepository,
            DocumentRepository documentRepository,
            FileRepository fileRepository,
            TaskRule taskRule,
            IdentityRule identityRule) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
        this.taskRule = taskRule;
        this.identityRule = identityRule;
    }

    @Transactional
    public AppResult<DocumentUploadResult> upload(String credential, UploadDocumentCommand command) {
        Auth auth = authorize(credential, MANAGE_IDENTITIES, MANAGE_FORBIDDEN);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        if (command == null) {
            return AppResult.fail("请上传文书文件");
        }
        String fileId = command.fileId();
        String fileName = command.fileName();
        String format = command.format();
        String content = command.content();
        byte[] bytes;
        if (fileId != null && !fileId.isBlank()) {
            StoredFile stored = fileRepository.find(fileId).orElse(null);
            if (stored == null) {
                return AppResult.fail("文件不存在");
            }
            fileName = firstNonBlank(stored.fileName(), fileName);
            format = firstNonBlank(stored.format(), format);
            if (content == null || content.isBlank()) {
                content = stored.content() == null ? "" : new String(stored.content(), StandardCharsets.UTF_8);
            }
            bytes = stored.content() == null ? content.getBytes(StandardCharsets.UTF_8) : stored.content();
        } else {
            if (content == null || content.isBlank()) {
                return AppResult.fail("请上传文书文件");
            }
            bytes = content.getBytes(StandardCharsets.UTF_8);
        }
        String documentType = taskRule.recognizeDocumentType(command.type(), bytes);
        if (documentType == null || documentType.isBlank()) {
            documentType = command.type();
        }
        CaseInfo caseInfo = taskRule.parseCaseInfo(content, documentType);
        String caseNo = firstNonBlank(
                caseInfo == null ? null : caseInfo.caseNo(),
                command.caseNo());
        caseInfo = withCaseNo(caseInfo, caseNo);
        if (caseNo != null) {
            Document existing = documentRepository.findByCaseNo(caseNo).orElse(null);
            if (existing != null) {
                return AppResult.ok(duplicateResult(existing));
            }
        }
        LocalDateTime now = LocalDateTime.now();
        String storedFileId = fileId;
        if (storedFileId == null || storedFileId.isBlank()) {
            storedFileId = fileRepository.save(fileName, format, bytes, auth.user().accountId(), now, "DOCUMENT");
        }
        String title = firstNonBlank(
                caseInfo == null ? null : caseInfo.caseName(),
                command.title(),
                fileName);
        Document document = new Document(
                null,
                title,
                caseNo,
                documentType,
                content,
                storedFileId,
                auth.user().accountId(),
                now,
                null,
                auth.user().system());
        String documentId = documentRepository.save(document);
        documentRepository.saveCaseInfo(documentId, caseInfo, true, false, auth.user().accountId(), now);
        return AppResult.ok(new DocumentUploadResult(
                documentId,
                title,
                caseNo,
                reasonText(content, documentType),
                CaseInfoView.from(caseInfo),
                false,
                null));
    }

    @Transactional(readOnly = true)
    public AppResult<DocumentDetailView> detail(String credential, String documentId) {
        Auth auth = authorize(credential, VIEW_IDENTITIES, VIEW_FORBIDDEN);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return AppResult.fail("文书不存在");
        }
        CaseInfo caseInfo = documentRepository.findCaseInfo(documentId).orElse(null);
        return AppResult.ok(new DocumentDetailView(
                document.documentId(),
                document.title(),
                document.caseNo(),
                document.documentType(),
                reasonText(document.content(), document.documentType()),
                CaseInfoView.from(caseInfo),
                document.uploaderId(),
                document.uploadedAt()));
    }

    @Transactional
    public AppResult<Void> saveCaseInfo(String credential, String documentId, SaveCaseInfoCommand command) {
        Auth auth = authorize(credential, MANAGE_IDENTITIES, MANAGE_FORBIDDEN);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        if (command == null || command.caseInfo() == null) {
            return AppResult.fail("案件信息不能为空");
        }
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return AppResult.fail("文书不存在");
        }
        CaseInfo caseInfo = command.caseInfo().toEntity();
        List<String> missing = taskRule.checkCaseInfo(document.documentType(), caseInfo);
        if (missing != null && !missing.isEmpty()) {
            return AppResult.fail(String.join("、", missing));
        }
        documentRepository.saveCaseInfo(documentId, caseInfo, false, true, auth.user().accountId(), LocalDateTime.now());
        return AppResult.ok(null);
    }

    @Transactional(readOnly = true)
    public AppResult<CaseInfoView> caseInfo(String credential, String documentId) {
        Auth auth = authorize(credential, VIEW_IDENTITIES, VIEW_FORBIDDEN);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        if (documentRepository.findById(documentId).isEmpty()) {
            return AppResult.fail("文书不存在");
        }
        CaseInfo caseInfo = documentRepository.findCaseInfo(documentId).orElse(null);
        if (caseInfo == null) {
            return AppResult.fail("案件信息不存在");
        }
        return AppResult.ok(CaseInfoView.from(caseInfo));
    }

    @Transactional(readOnly = true)
    public AppResult<PageResult<DocumentListItemView>> list(
            String credential,
            String keyword,
            SystemType system,
            int page,
            int size) {
        Auth auth = authorize(credential, MANAGE_IDENTITIES, MANAGE_FORBIDDEN);
        if (auth.failure() != null) {
            return AppResult.fail(auth.failure());
        }
        int safePage = page <= 0 ? 1 : page;
        int safeSize = size <= 0 ? 20 : size;
        PageResult<Document> found = documentRepository.search(keyword, system, safePage, safeSize);
        if (found == null) {
            return AppResult.ok(new PageResult<>(List.of(), 0, safePage, safeSize));
        }
        List<DocumentListItemView> records = found.records().stream()
                .map(document -> new DocumentListItemView(
                        document.documentId(),
                        document.title(),
                        document.caseNo(),
                        document.documentType(),
                        null))
                .toList();
        return AppResult.ok(new PageResult<>(records, found.total(), found.page(), found.size()));
    }

    private DocumentUploadResult duplicateResult(Document existing) {
        CaseInfo caseInfo = documentRepository.findCaseInfo(existing.documentId()).orElse(null);
        return new DocumentUploadResult(
                existing.documentId(),
                existing.title(),
                existing.caseNo(),
                reasonText(existing.content(), existing.documentType()),
                CaseInfoView.from(caseInfo),
                true,
                existing.documentId());
    }

    private String reasonText(String content, String documentType) {
        if (content == null || content.isBlank()) {
            return "";
        }
        TaskRule.TextRange range = taskRule.locateReasoning(content, documentType);
        if (range == null) {
            return "";
        }
        int start = Math.max(0, Math.min(content.length(), range.start()));
        int end = Math.max(start, Math.min(content.length(), range.end()));
        return content.substring(start, end);
    }

    private Auth authorize(String credential, Set<IdentityType> allowed, String forbiddenMessage) {
        LoginUser user = sessionRepository.findByCredential(credential).orElse(null);
        if (user == null) {
            return new Auth(null, null, "登录已失效，请重新登录");
        }
        Account account = accountRepository.findById(user.accountId()).orElse(null);
        if (account == null) {
            return new Auth(user, null, "账号不存在");
        }
        if (!identityRule.isEnabled(account)) {
            return new Auth(user, account, "账号已停用，请联系管理员");
        }
        IdentityType identity = user.identity();
        if (identity == null
                || !allowed.contains(identity)
                || !identityRule.hasRole(account, identity)) {
            return new Auth(user, account, forbiddenMessage);
        }
        return new Auth(user, account, null);
    }

    private static CaseInfo withCaseNo(CaseInfo caseInfo, String caseNo) {
        if (caseInfo == null) {
            return new CaseInfo(caseNo, null, null, null, null, null, null, null);
        }
        if (Objects.equals(caseInfo.caseNo(), caseNo)) {
            return caseInfo;
        }
        return new CaseInfo(
                caseNo,
                caseInfo.caseName(),
                caseInfo.court(),
                caseInfo.judge(),
                caseInfo.parties(),
                caseInfo.cause(),
                caseInfo.filingDate(),
                caseInfo.extra());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record Auth(LoginUser user, Account account, String failure) {
    }
}
