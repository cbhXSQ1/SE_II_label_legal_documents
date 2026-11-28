package com.seii.legal.rule.task;

import com.seii.legal.common.enums.TaskStatus;
import com.seii.legal.data.entity.AnnotationTask;
import com.seii.legal.data.entity.CaseInfo;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskProgress;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRule {
    String recognizeDocumentType(String selectedType, byte[] file);

    TextRange locateReasoning(String text, String documentType);

    CaseInfo parseCaseInfo(String text, String documentType);

    List<String> checkCaseInfo(String documentType, CaseInfo info);

    List<String> checkPublish(AnnotationTask draft);

    TaskStatus nextStatus(TaskStatus current, TaskAction action);

    TaskProgress progress(String taskId, List<Submission> submissions);

    boolean canSubmit(LocalDateTime deadline, LocalDateTime now);

    boolean hasEnoughAnnotators(List<String> annotatorIds);

    boolean hasSingleArbiter(String arbiterId);

    record TextRange(int start, int end) {
    }

    enum TaskAction {
        CREATE, PUBLISH, UPDATE, WITHDRAW, REOPEN, SUBMIT, RETURN, DECIDE, CONFIRM
    }
}
