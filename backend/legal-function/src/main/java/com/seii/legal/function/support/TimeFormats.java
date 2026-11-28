package com.seii.legal.function.support;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 统一时间工具：使用学校所在时区（Asia/Shanghai）与“yyyy-MM-dd HH:mm:ss”格式。
 */
public final class TimeFormats {

    public static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Shanghai");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeFormats() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(SCHOOL_ZONE);
    }

    public static String format(LocalDateTime time) {
        return time == null ? "" : DATE_TIME.format(time);
    }
}
