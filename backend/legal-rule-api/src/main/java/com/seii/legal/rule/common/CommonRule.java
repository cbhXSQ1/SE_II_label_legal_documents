package com.seii.legal.rule.common;

import java.util.List;

public interface CommonRule {
    List<String> missingFields(Object form, List<String> required);

    boolean readableFile(String fileName, String format);

    String percent(double value);

    boolean duplicateRequest(String requestId, List<String> processed);
}
