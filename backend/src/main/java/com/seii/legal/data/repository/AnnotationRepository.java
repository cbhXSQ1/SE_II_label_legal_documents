package com.seii.legal.data.repository;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.data.entity.Submission;
import java.util.List;

/** Formal submissions; a null annotatorId selects all annotators in the requested round. */
public interface AnnotationRepository {
    String saveSubmission(Submission submission, String requestId);
    List<Submission> findSubmissions(String taskId, String documentId, RoundType round, String annotatorId);
    List<Submission> findSubmissionsByRound(String roundId, String annotatorId);
    void updateSubmissionStatus(String submissionId, SubmissionStatus status, String returnReason, String auditNote);
}
