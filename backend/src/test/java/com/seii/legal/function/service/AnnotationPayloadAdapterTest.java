package com.seii.legal.function.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationPayloadAdapterTest {

    private final AnnotationPayloadAdapter adapter = new AnnotationPayloadAdapter(new ObjectMapper());

    @Test
    void adaptsBothNestedAndFlatPayloadsToUnifiedJson() {
        AnnotationPayloadAdapter.AnnotationContent flat = new AnnotationPayloadAdapter.AnnotationContent(
                List.of(Map.of("id", "s1", "text", "依法成立的合同，自成立时生效。")),
                List.of(Map.of("id", "P1", "text", "依法成立的合同，自成立时生效。", "type", "GM-L")),
                null,
                null
        );

        String json = adapter.toStorageJson(null, flat);
        AnnotationPayloadAdapter.AnnotationContent restored = adapter.fromStorageJson(json);

        assertEquals(1, restored.pieces().size());
        assertEquals("P1", restored.segments().get(0).get("id"));
        assertNotNull(restored.relations());
        assertTrue(restored.relations().isEmpty());
        assertNotNull(restored.diagram());
        assertTrue(restored.diagram().isEmpty());
    }
}
