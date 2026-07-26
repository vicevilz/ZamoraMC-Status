package com.vicevil.zamoramcstatus.paper;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

final class StatusCache {
    private volatile Map<String, Boolean> values = Map.of();

    void replace(Map<String, Boolean> snapshot) {
        Map<String, Boolean> normalized = new HashMap<>();
        snapshot.forEach((name, online) -> normalized.put(normalize(name), online));
        values = Map.copyOf(normalized);
    }

    void clear() {
        values = Map.of();
    }

    boolean get(String serverName, boolean fallback) {
        return values.getOrDefault(normalize(serverName), fallback);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
