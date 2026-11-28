package com.seii.legal.rule.guide;

import com.seii.legal.data.entity.GuideVersion;
import java.util.List;

public interface GuideRule {
    boolean canSaveVersion(String version, List<GuideVersion> existing);

    GuideContent readLabels(byte[] guideFile, String format);

    void ensureReadOnly(GuideVersion version);

    void registerReference(String objectType, String objectId, String version);

    record GuideContent(
            List<String> nonPropositionTypes,
            List<String> propositionTypes,
            List<String> relationTypes,
            List<String> diagramRules
    ) {
    }
}
