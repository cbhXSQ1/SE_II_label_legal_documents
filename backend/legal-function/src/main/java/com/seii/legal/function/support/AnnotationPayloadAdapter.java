package com.seii.legal.function.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AnnotationPayloadAdapter {

    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public AnnotationPayloadAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record AnnotationContent(
            List<Map<String, Object>> pieces,
            List<Map<String, Object>> segments,
            List<Map<String, Object>> relations,
            List<Map<String, Object>> diagram
    ) {
    }

    public String toStorageJson(AnnotationContent nested, AnnotationContent flat) {
        try {
            return objectMapper.writeValueAsString(merge(nested, flat));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("标注内容序列化失败");
        }
    }

    public AnnotationContent fromStorageJson(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return empty();
        }
        try {
            AnnotationContent parsed = objectMapper.readValue(contentJson, AnnotationContent.class);
            return merge(parsed, null);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("标注内容解析失败");
        }
    }

    public String listToJson(List<Map<String, Object>> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("标注内容序列化失败");
        }
    }

    public List<Map<String, Object>> listFromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(json, LIST_TYPE);
            return parsed == null ? List.of() : parsed;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("标注内容解析失败");
        }
    }

    public AnnotationContent fromDraftJson(
            String cutJson,
            String elementsJson,
            String relationsJson,
            String diagramJson) {
        return new AnnotationContent(
                listFromJson(cutJson),
                listFromJson(elementsJson),
                listFromJson(relationsJson),
                listFromJson(diagramJson));
    }

    public AnnotationContent merge(AnnotationContent nested, AnnotationContent flat) {
        AnnotationContent effective = nested != null ? nested : flat;
        if (effective == null) {
            return empty();
        }
        return new AnnotationContent(
                valueOrEmpty(effective.pieces()),
                valueOrEmpty(effective.segments()),
                valueOrEmpty(effective.relations()),
                valueOrEmpty(effective.diagram()));
    }

    public AnnotationContent empty() {
        return new AnnotationContent(List.of(), List.of(), List.of(), List.of());
    }

    private static List<Map<String, Object>> valueOrEmpty(List<Map<String, Object>> value) {
        return value == null ? List.of() : value;
    }
}
