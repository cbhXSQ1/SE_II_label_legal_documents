package com.seii.legal.rule.annotation;

import com.seii.legal.data.entity.Agreement;
import com.seii.legal.data.entity.Element;

import java.util.List;

public interface ElementRule {
    String classifyNonProposition(String paragraph, String guideVersion);

    String classifyProposition(String paragraph, String guideVersion);

    String classifyPropositionSubType(String paragraph, String propositionType, String guideVersion);

    boolean isPropositionCandidate(Element element);

    List<String> findUnassigned(List<Element> elements);

    Agreement compareElements(List<Element> a, List<Element> b);
}
