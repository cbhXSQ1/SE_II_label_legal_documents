package com.seii.legal.common.page;

import java.util.List;

/**
 * 统一分页结果，页码从 1 开始，默认每页 20 条。
 */
public record PageResult<T>(List<T> records, long total, int page, int size) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, normalizePage(page), normalizeSize(size));
    }

    public static int normalizePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    public static int normalizeSize(Integer size) {
        return (size == null || size < 1) ? DEFAULT_SIZE : size;
    }
}
