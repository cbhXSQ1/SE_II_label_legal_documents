package com.seii.legal.function.dto.teacher;

import java.util.List;

public record MaterialCommand(
        String id,
        String title,
        String type,
        String content,
        String fileId,
        String guideVersion,
        List<String> pointIds,
        String classId,
        Boolean publish
) {
}
