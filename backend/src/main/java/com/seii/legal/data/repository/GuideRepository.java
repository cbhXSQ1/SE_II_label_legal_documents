package com.seii.legal.data.repository;

import com.seii.legal.data.entity.GuideVersion;
import com.seii.legal.data.entity.ReadingRecord;

import java.util.List;
import java.util.Optional;

public interface GuideRepository {
    Optional<GuideVersion> findVersion(String version);

    List<GuideVersion> findAvailable();

    String saveVersion(GuideVersion version);

    void saveReference(String objectType, String objectId, String version);

    Optional<String> findReference(String objectType, String objectId);

    void saveReadingRecord(ReadingRecord record);

    Optional<ReadingRecord> findReadingRecord(String userId, String version);
}
