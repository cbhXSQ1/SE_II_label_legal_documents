CREATE TABLE task_round (
    round_id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    round_type VARCHAR(20) NOT NULL,
    sequence_no INT NOT NULL,
    opened_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6),
    open_slot TINYINT GENERATED ALWAYS AS (IF(closed_at IS NULL, 1, NULL)) STORED,
    UNIQUE KEY uq_round_open (task_id, document_id, round_type, open_slot),
    UNIQUE KEY uq_round_sequence (task_id, document_id, round_type, sequence_no),
    CHECK (closed_at IS NULL OR closed_at >= opened_at)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

CREATE TABLE annotation_submission (
    submission_id VARCHAR(64) PRIMARY KEY,
    round_id VARCHAR(64) NOT NULL,
    annotator_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    guide_version VARCHAR(128) NOT NULL,
    content LONGTEXT NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    return_reason TEXT,
    active_slot TINYINT GENERATED ALWAYS AS (IF(status IN ('SUBMITTED', 'LOCKED'), 1, NULL)) STORED,
    UNIQUE KEY uq_submission_request (annotator_id, request_id),
    UNIQUE KEY uq_current_submission (round_id, annotator_id, active_slot),
    CONSTRAINT fk_submission_round FOREIGN KEY (round_id) REFERENCES task_round(round_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

CREATE TABLE submission_status_log (
    change_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id VARCHAR(64) NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason TEXT,
    audit_note TEXT,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_change_submission FOREIGN KEY (submission_id) REFERENCES annotation_submission(submission_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;
