package com.example.eventgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class Logging {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SERVICE = "event-gateway";

    public static void log(String level, String traceId, String message) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", Instant.now().toString());
            entry.put("level", level);
            entry.put("service", SERVICE);
            entry.put("traceId", traceId);
            entry.put("message", message);
            System.out.println(OBJECT_MAPPER.writeValueAsString(entry));
        } catch (Exception ignored) {
            System.out.println("{\"msg\":\"log-fail\"}");
        }
    }
}
