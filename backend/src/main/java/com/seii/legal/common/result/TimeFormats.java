package com.seii.legal.common.result;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 统一时间格式（年-月-日 时:分:秒，按学校所在时区 Asia/Shanghai）。
 */
public final class TimeFormats {

    public static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Shanghai");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeFormats() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(SCHOOL_ZONE);
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(DATE_TIME);
    }

    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        try {
            return LocalDateTime.parse(trimmed, DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(trimmed, DATE_MINUTE);
            } catch (DateTimeParseException ex) {
                return LocalDateTime.parse(trimmed);
            }
        }
    }
}
