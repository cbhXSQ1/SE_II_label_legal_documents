package com.seii.legal.data.mysql;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.DataConflictException;
import com.seii.legal.data.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTaskRepository implements TaskRepository {
    private static final String SELECT = """
            SELECT round_id, task_id, document_id, round_type, sequence_no, opened_at, closed_at
            FROM task_round
            """;
    private static final RowMapper<TaskRound> ROW = (rs, index) -> {
        LocalDateTime closed = rs.getObject("closed_at", LocalDateTime.class);
        return new TaskRound(rs.getString("round_id"), rs.getString("task_id"),
                rs.getString("document_id"), RoundType.valueOf(rs.getString("round_type")),
                rs.getInt("sequence_no"), closed == null ? "OPEN" : "CLOSED",
                rs.getObject("opened_at", LocalDateTime.class), closed);
    };

    private final JdbcTemplate jdbc;

    public JdbcTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String saveRound(TaskRound round) {
        if (round.closedAt() != null || (round.status() != null && !"OPEN".equals(round.status()))) {
            throw new IllegalArgumentException("新轮次必须处于打开状态");
        }
        String id = round.roundId() == null ? UUID.randomUUID().toString() : round.roundId();
        try {
            jdbc.update("""
                    INSERT INTO task_round(round_id, task_id, document_id, round_type, sequence_no, opened_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, round.taskId(), round.documentId(), round.type().name(), round.sequence(), round.openedAt());
        } catch (DuplicateKeyException duplicate) {
            throw new DataConflictException("轮次标识或序号重复，或同类型轮次尚未关闭");
        }
        return id;
    }

    @Override
    public Optional<TaskRound> findRound(String roundId) {
        return jdbc.query(SELECT + "WHERE round_id = ?", ROW, roundId).stream().findFirst();
    }

    @Override
    public Optional<TaskRound> findCurrentRound(String taskId, String documentId, RoundType type) {
        return jdbc.query(SELECT + "WHERE task_id = ? AND document_id = ? AND round_type = ? AND closed_at IS NULL",
                ROW, taskId, documentId, type.name()).stream().findFirst();
    }

    @Override
    public List<TaskRound> findRounds(String taskId, String documentId) {
        return jdbc.query(SELECT + "WHERE task_id = ? AND document_id = ? ORDER BY sequence_no, opened_at, round_id",
                ROW, taskId, documentId);
    }

    @Override
    public void closeRound(String roundId, LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("关闭轮次需要关闭时间");
        }
        int changed = jdbc.update("""
                UPDATE task_round SET closed_at = ?
                WHERE round_id = ? AND closed_at IS NULL AND opened_at <= ?
                """, time, roundId, time);
        if (changed != 1) {
            throw new DataConflictException("轮次不存在、已关闭或关闭时间早于开启时间");
        }
    }
}
