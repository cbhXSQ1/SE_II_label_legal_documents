package com.seii.legal.function.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationPayloadAdapterTest {

    private final AnnotationPayloadAdapter adapter = new AnnotationPayloadAdapter(new ObjectMapper());

    @Test
    void nestedContentWinsWhenBothShapesProvided() {
        AnnotationPayloadAdapter.AnnotationContent nested = new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "nested-piece")), List.of(), List.of(), List.of());
        AnnotationPayloadAdapter.AnnotationContent flat = new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "flat-piece")), List.of(), List.of(), List.of());

        String json = adapter.toStorageJson(nested, flat);
        AnnotationPayloadAdapter.AnnotationContent parsed = adapter.fromStorageJson(json);

        assertEquals("nested-piece", parsed.pieces().get(0).get("id"));
    }

    @Test
    void flatContentUsedWhenNestedIsNull() {
        AnnotationPayloadAdapter.AnnotationContent flat = new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "flat-piece")),
                List.of(Map.of("id", "flat-segment")),
                List.of(Map.of("id", "flat-relation")),
                List.of(Map.of("id", "flat-diagram")));

        String json = adapter.toStorageJson(null, flat);
        AnnotationPayloadAdapter.AnnotationContent parsed = adapter.fromStorageJson(json);

        assertEquals("flat-piece", parsed.pieces().get(0).get("id"));
        assertEquals("flat-segment", parsed.segments().get(0).get("id"));
        assertEquals("flat-relation", parsed.relations().get(0).get("id"));
        assertEquals("flat-diagram", parsed.diagram().get(0).get("id"));
    }

    @Test
    void missingListsAreNormalizedToEmptyLists() {
        AnnotationPayloadAdapter.AnnotationContent partial = new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "p1")), null, null, null);

        String json = adapter.toStorageJson(partial, null);
        AnnotationPayloadAdapter.AnnotationContent parsed = adapter.fromStorageJson(json);

        assertEquals(1, parsed.pieces().size());
        assertTrue(parsed.segments().isEmpty());
        assertTrue(parsed.relations().isEmpty());
        assertTrue(parsed.diagram().isEmpty());
    }

    @Test
    void nullNestedAndFlatBecomeEmptyContent() {
        AnnotationPayloadAdapter.AnnotationContent parsed = adapter.fromStorageJson(adapter.toStorageJson(null, null));

        assertTrue(parsed.pieces().isEmpty());
        assertTrue(parsed.segments().isEmpty());
        assertTrue(parsed.relations().isEmpty());
        assertTrue(parsed.diagram().isEmpty());
    }

    @Test
    void blankStorageJsonBecomesEmptyContent() {
        assertTrue(adapter.fromStorageJson(null).pieces().isEmpty());
        assertTrue(adapter.fromStorageJson("  ").segments().isEmpty());
    }

    @Test
    void draftPartsAreCombinedIntoContent() {
        AnnotationPayloadAdapter.AnnotationContent content = adapter.fromDraftJson(
                "[{\"id\":\"s1\"}]",
                "[{\"id\":\"P1\"}]",
                "[{\"id\":\"R1\"}]",
                "[{\"id\":\"dg1\"}]");

        assertEquals("s1", content.pieces().get(0).get("id"));
        assertEquals("P1", content.segments().get(0).get("id"));
        assertEquals("R1", content.relations().get(0).get("id"));
        assertEquals("dg1", content.diagram().get(0).get("id"));
    }

    @Test
    void draftPartsTolerateBlankValues() {
        AnnotationPayloadAdapter.AnnotationContent content = adapter.fromDraftJson(null, "", " ", null);

        assertTrue(content.pieces().isEmpty());
        assertTrue(content.segments().isEmpty());
        assertTrue(content.relations().isEmpty());
        assertTrue(content.diagram().isEmpty());
    }

    @Test
    void listJsonRoundTripsAndNormalizesNull() {
        assertEquals("[]", adapter.listToJson(null));
        assertTrue(adapter.listFromJson(null).isEmpty());
        assertTrue(adapter.listFromJson("").isEmpty());
        assertEquals("P1", adapter.listFromJson(adapter.listToJson(List.of(Map.of("id", "P1"))))
                .get(0).get("id"));
    }

    @Test
    void malformedJsonIsRejected() {
        IllegalStateException storageError = assertThrows(IllegalStateException.class,
                () -> adapter.fromStorageJson("{not-json"));
        assertEquals("标注内容解析失败", storageError.getMessage());

        IllegalStateException listError = assertThrows(IllegalStateException.class,
                () -> adapter.listFromJson("{not-json"));
        assertEquals("标注内容解析失败", listError.getMessage());
    }
}
