package com.seii.legal.function.dto.document;

import com.seii.legal.data.entity.CaseInfo;

public record CaseInfoView(
        String caseNo,
        String caseName,
        String court,
        String judge,
        String parties,
        String cause,
        String filingDate,
        String extra
) {
    public CaseInfo toEntity() {
        return new CaseInfo(caseNo, caseName, court, judge, parties, cause, filingDate, extra);
    }

    public static CaseInfoView from(CaseInfo info) {
        if (info == null) {
            return null;
        }
        return new CaseInfoView(
                info.caseNo(),
                info.caseName(),
                info.court(),
                info.judge(),
                info.parties(),
                info.cause(),
                info.filingDate(),
                info.extra());
    }
}
