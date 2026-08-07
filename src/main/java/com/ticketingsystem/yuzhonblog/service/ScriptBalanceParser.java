package com.ticketingsystem.yuzhonblog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScriptBalanceParser {

    /**
     * Parse balance info from an API response using a safe JSON path expression.
     *
     * Supported formats:
     *   - Simple path: "data.balance" -> response["data"]["balance"]
     *   - Boolean path: "data.available" (interpreted as boolean flag)
     *   - Array index: "data.items[0].balance"
     *
     * Expected config fields in balanceScript (comma-separated):
     *   balance=data.balance, currency=data.currency, available=data.available
     *
     * @return parsed balance info, or null if parsing fails
     */
    @SuppressWarnings("unchecked")
    public BalanceParseResult execute(String script, Map<String, Object> response) {
        if (script == null || script.isBlank() || response == null) return null;

        try {
            Map<String, String> paths = parseConfig(script);
            String balance = extractString(response, paths.getOrDefault("balance", ""));
            String currency = extractString(response, paths.getOrDefault("currency", "CNY"));
            if (currency.isBlank()) currency = "CNY";
            boolean isAvailable = !paths.containsKey("available") || extractBoolean(response, paths.get("available"));
            return new BalanceParseResult(balance, currency, isAvailable);
        } catch (Exception e) {
            log.warn("Balance parse failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse "balance=data.balance, currency=data.currency" into a map.
     */
    private Map<String, String> parseConfig(String script) {
        Map<String, String> paths = new java.util.HashMap<>();
        for (String part : script.split(",")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && eq < trimmed.length() - 1) {
                paths.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        }
        return paths;
    }

    /**
     * Walk a JSON-like map structure using dot-separated path.
     * Supports: "data.balance", "data.items[0].name"
     */
    @SuppressWarnings("unchecked")
    private Object extractValue(Map<String, Object> root, String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (current == null) return null;
            // Handle array index: items[0]
            int bracket = part.indexOf('[');
            if (bracket > 0) {
                String key = part.substring(0, bracket);
                int idx = Integer.parseInt(part.substring(bracket + 1, part.indexOf(']')));
                current = getMapValue(current, key);
                if (current instanceof List<?> list) {
                    current = idx < list.size() ? list.get(idx) : null;
                } else {
                    return null;
                }
            } else {
                current = getMapValue(current, part);
            }
        }
        return current;
    }

    private Object getMapValue(Object obj, String key) {
        if (obj instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private String extractString(Map<String, Object> root, String path) {
        Object val = extractValue(root, path);
        return val != null ? String.valueOf(val) : "";
    }

    private boolean extractBoolean(Map<String, Object> root, String path) {
        Object val = extractValue(root, path);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return "true".equalsIgnoreCase(s) || "1".equals(s);
        return true;
    }

    public record BalanceParseResult(String balance, String currency, boolean isAvailable) {}
}
