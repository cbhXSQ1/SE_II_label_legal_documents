package com.seii.legal.rule.annotation;

import com.seii.legal.data.entity.Agreement;

import java.util.List;

public interface SegmentationRule {
    List<Integer> cutPoints(String text, String guideVersion);

    List<String> numberParagraphs(List<Integer> cutPoints, String text);

    List<Integer> renumber(List<Integer> cutPoints, CutOperation operation);

    List<Integer> findUncut(List<Integer> cutPoints, String text);

    Agreement compareCut(List<Integer> cutA, List<Integer> cutB);

    enum CutOperation {
        MERGE, SPLIT
    }
}
