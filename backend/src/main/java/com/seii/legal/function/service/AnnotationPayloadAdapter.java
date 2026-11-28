package com.seii.legal.function.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seii.legal.common.result.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 适配器模式：在功能层内部兼容嵌套 content 对象与顶层平铺 4 字段，并与数据层 Submission.content 字符串互转。
 */
@Component
@RequiredArgsConstructor
public class AnnotationPayloadAdapter {

    private final ObjectMapper objectMapper;

    public record AnnotationContent(
            List<Map<String, Object>> pieces,
            List<Map<String, Object>> segments,
            List<Map<String, Object>> relations,
            List<Map<String, Object>> diagram
    ) {
        public static AnnotationContent empty() {
            return new AnnotationContent(List.of(), List.of(), List.of(), List.of());
        }
    }

    public AnnotationContent normalize(AnnotationContent nested, AnnotationContent flat) {
        AnnotationContent effective = (nested != null) ? nested : flat;
        if (effective == null) {
            return AnnotationContent.empty();
        }
        return new AnnotationContent(
                effective.pieces() != null ? effective.pieces() : List.of(),
                effective.segments() != null ? effective.segments() : List.of(),
                effective.relations() != null ? effective.relations() : List.of(),
                effective.diagram() != null ? effective.diagram() : List.of()
        );
    }

    public String toStorageJson(AnnotationContent nested, AnnotationContent flat) {
        AnnotationContent normalized = normalize(nested, flat);
        return writeJson(normalized);
    }

    public String writeJson(Object value) {
        if (value == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("标注内容序列化失败");
        }
    }

    public AnnotationContent fromStorageJson(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return AnnotationContent.empty();
        }
        try {
            AnnotationContent parsed = objectMapper.readValue(contentJson, AnnotationContent.class);
            return normalize(parsed, null);
        } catch (JsonProcessingException e) {
            throw new BusinessException("标注内容解析失败");
        }
    }
}
