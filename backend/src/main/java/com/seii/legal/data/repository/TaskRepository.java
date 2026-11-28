package com.seii.legal.data.repository;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.data.entity.TaskRound;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Task operations currently available for round lifecycle management. */
public interface TaskRepository {
    String saveRound(TaskRound round);
    Optional<TaskRound> findRound(String roundId);
    Optional<TaskRound> findCurrentRound(String taskId, String documentId, RoundType type);
    List<TaskRound> findRounds(String taskId, String documentId);
    void closeRound(String roundId, LocalDateTime time);
}
