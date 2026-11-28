package com.seii.legal.rule.adjudication;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.data.entity.AdjudicationRecord;
import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.CutVersion;
import com.seii.legal.data.entity.DiagramReview;
import com.seii.legal.data.entity.DiffItem;

import java.util.List;

public interface AdjudicationRule {
    Agreement difference(Object a, Object b, RoundType round);

    List<DiffItem> classify(Object a, Object b, RoundType round);

    boolean aboveEighty(Agreement agreement);

    List<String> fourPrinciples(List<DiffItem> cutConflicts);

    List<String> checkFinalVersion(List<AdjudicationRecord> decisions, List<DiagramReview> reviews, CutVersion unifiedCut);

    boolean singleAnnotator(int submissionCount);
}
