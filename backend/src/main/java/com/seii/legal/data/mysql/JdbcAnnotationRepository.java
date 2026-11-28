package com.seii.legal.data.mysql;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.DataConflictException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcAnnotationRepository implements AnnotationRepository {
    private static final String SELECT = """
            SELECT s.submission_id, r.task_id, r.document_id, r.round_type, s.round_id,
                   s.annotator_id, s.submitted_at, s.guide_version, s.content, s.status, s.return_reason
            FROM annotation_submission s JOIN task_round r ON r.round_id = s.round_id
            """;
    private static final RowMapper<Submission> ROW = (rs, index) -> new Submission(
            rs.getString("submission_id"), rs.getString("task_id"), rs.getString("document_id"),
            RoundType.valueOf(rs.getString("round_type")), rs.getString("round_id"),
            rs.getString("annotator_id"), rs.getObject("submitted_at", LocalDateTime.class),
            rs.getString("guide_version"), rs.getString("content"),
            SubmissionStatus.valueOf(rs.getString("status")), rs.getString("return_reason"));

    private final JdbcTemplate jdbc;

    public JdbcAnnotationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public String saveSubmission(Submission submission, String requestId) {
        if (requestId == null || requestId.isBlank() || submission.status() != SubmissionStatus.SUBMITTED) {
            throw new IllegalArgumentException("正式提交需要请求标识及 SUBMITTED 状态");
        }
        // Serialize new submissions with round closure. Replays are valid after closure.
        List<Boolean> open = jdbc.query("""
                SELECT closed_at IS NULL FROM task_round
                WHERE round_id = ? AND task_id = ? AND document_id = ? AND round_type = ? FOR UPDATE
                """, (rs, i) -> rs.getBoolean(1), submission.roundId(), submission.taskId(),
                submission.documentId(), submission.round().name());
        if (open.isEmpty()) {
            throw new DataConflictException("轮次与任务、文书或轮次类型不匹配");
        }

        Optional<String> receipt = findReceipt(submission, requestId, false);
        if (receipt.isPresent()) {
            return receipt.get();
        }
        if (!open.get(0)) {
            return findReceipt(submission, requestId, true)
                    .orElseThrow(() -> new DataConflictException("轮次已关闭"));
        }

        String id = submission.submissionId() == null ? UUID.randomUUID().toString() : submission.submissionId();
        try {
            jdbc.update("""
                    INSERT INTO annotation_submission
                        (submission_id, round_id, annotator_id, request_id, guide_version, content, submitted_at, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'SUBMITTED')
                    """, id, submission.roundId(), submission.annotatorId(), requestId, submission.guideVersion(),
                    submission.content(), submission.submittedAt());
        } catch (DuplicateKeyException duplicate) {
            // A transaction that started earlier may have an older consistent-read snapshot.
            Optional<String> existing = findReceipt(submission, requestId, true);
            if (existing.isPresent()) {
                return existing.get();
            }
            boolean alreadySubmitted = !jdbc.queryForList("""
                    SELECT submission_id FROM annotation_submission
                    WHERE round_id = ? AND annotator_id = ? AND active_slot = 1 FOR SHARE
                    """, String.class, submission.roundId(), submission.annotatorId()).isEmpty();
            throw new DataConflictException(alreadySubmitted ? "本轮已经提交" : "提交标识重复");
        }
        return id;
    }

    private Optional<String> findReceipt(Submission submission, String requestId, boolean currentRead) {
        String sql = """
                SELECT submission_id, round_id, guide_version, content FROM annotation_submission
                WHERE annotator_id = ? AND request_id = ?
                """;
        return jdbc.query(sql + (currentRead ? "FOR SHARE" : ""), (rs, i) -> {
            if (!submission.roundId().equals(rs.getString("round_id"))
                    || !submission.guideVersion().equals(rs.getString("guide_version"))
                    || !submission.content().equals(rs.getString("content"))) {
                throw new DataConflictException("同一请求标识携带了不同的提交内容");
            }
            return rs.getString("submission_id");
        }, submission.annotatorId(), requestId).stream().findFirst();
    }

    @Override
    public List<Submission> findSubmissions(String taskId, String documentId, RoundType round, String annotatorId) {
        return jdbc.query(SELECT + """
                WHERE r.task_id = ? AND r.document_id = ? AND r.round_type = ? AND r.closed_at IS NULL
                  AND s.active_slot = 1 AND (? IS NULL OR s.annotator_id = ?)
                ORDER BY s.submitted_at, s.submission_id
                """, ROW, taskId, documentId, round.name(), annotatorId, annotatorId);
    }

    @Override
    public List<Submission> findSubmissionsByRound(String roundId, String annotatorId) {
        return jdbc.query(SELECT + """
                WHERE s.round_id = ? AND (? IS NULL OR s.annotator_id = ?)
                ORDER BY s.submitted_at, s.submission_id
                """, ROW, roundId, annotatorId, annotatorId);
    }

    @Override
    @Transactional
    public void updateSubmissionStatus(String submissionId, SubmissionStatus status, String returnReason, String auditNote) {
        List<String> previous = jdbc.query("""
                SELECT s.status FROM task_round r JOIN annotation_submission s ON s.round_id = r.round_id
                WHERE s.submission_id = ? AND r.closed_at IS NULL FOR UPDATE
                """, (rs, i) -> rs.getString(1), submissionId);
        if (previous.isEmpty()) {
            throw new DataConflictException("提交不存在或轮次已关闭");
        }
        jdbc.update("UPDATE annotation_submission SET status = ?, return_reason = ? WHERE submission_id = ?",
                status.name(), returnReason, submissionId);
        jdbc.update("""
                INSERT INTO submission_status_log(submission_id, old_status, new_status, reason, audit_note)
                VALUES (?, ?, ?, ?, ?)
                """, submissionId, previous.get(0), status.name(), returnReason, auditNote);
    }
}
