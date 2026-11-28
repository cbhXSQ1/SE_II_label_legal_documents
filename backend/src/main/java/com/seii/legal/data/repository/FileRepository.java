package com.seii.legal.data.repository;

import com.seii.legal.data.entity.StoredFile;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FileRepository {
    String save(String fileName, String format, byte[] content, String uploaderId, LocalDateTime time, String type);

    Optional<StoredFile> find(String fileId);

    void delete(String fileId);
}
