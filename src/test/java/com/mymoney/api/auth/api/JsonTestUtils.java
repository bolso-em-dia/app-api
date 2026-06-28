package com.mymoney.api.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonTestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtils() {}

    public static String extractJsonValue(String json, String field) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        return root.get(field).asText();
    }
}
