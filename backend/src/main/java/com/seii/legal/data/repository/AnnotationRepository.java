package com.seii.legal.data.repository;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.Diagram;
import com.seii.legal.data.entity.Draft;
import com.seii.legal.data.entity.Element;
import com.seii.legal.data.entity.Relation;
import com.seii.legal.data.entity.Submission;

import java.util.List;
import java.util.Optional;

public interface AnnotationRepository {
    String saveCutVersion(CutVersion version);

    Optional<CutVersion> findCutVersion(String versionId);

    Optional<CutVersion> findCutVersion(String taskId, String documentId, String annotatorId, RoundType round);

    String saveElements(String cutVersionId, List<Element> elements);

    List<Element> findElements(String cutVersionId);

    String saveRelations(String elementVersionId, List<Relation> relations);

    List<Relation> findRelations(String elementVersionId);

    String saveDiagram(Diagram diagram);

    Optional<Diagram> findDiagram(String diagramId);

    Optional<Diagram> findDiagramByRelationVersion(String relationVersionId);

    void saveDraft(Draft draft);

    Optional<Draft> findDraft(String taskId, String documentId, String annotatorId);

    String saveSubmission(Submission submission, String requestId);

    List<Submission> findSubmissions(String taskId, String documentId, RoundType round, String annotatorId);

    void updateSubmissionStatus(String submissionId, SubmissionStatus status, String returnReason, String auditNote);
}
