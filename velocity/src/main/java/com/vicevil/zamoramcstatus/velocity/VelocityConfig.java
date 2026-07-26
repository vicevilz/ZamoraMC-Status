package com.vicevil.zamoramcstatus.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

record VelocityConfig(int checkIntervalSeconds, int pingTimeoutMilliseconds) {
    static VelocityConfig load(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve("config.yml");
        if (Files.notExists(file)) {
            try (InputStream defaults = VelocityConfig.class.getResourceAsStream("/config.yml")) {
                if (defaults == null) {
                    throw new IOException("Missing embedded Velocity config.yml");
                }
                Files.copy(defaults, file);
            }
        }
        Object parsed;
        try (InputStream input = Files.newInputStream(file)) {
            parsed = new Yaml().load(input);
        }
        if (!(parsed instanceof Map<?, ?> values)) {
            throw new IOException("Velocity config.yml must contain a YAML mapping");
        }
        int interval = boundedInt(values.get("check-interval-seconds"), 5, 1, 3_600);
        int timeout = boundedInt(values.get("ping-timeout-milliseconds"), 2_000, 100, 30_000);
        return new VelocityConfig(interval, timeout);
    }

    private static int boundedInt(Object value, int fallback, int minimum, int maximum) {
        if (value instanceof Number number) {
            return Math.max(minimum, Math.min(maximum, number.intValue()));
        }
        if (value instanceof String string) {
            try {
                return Math.max(minimum, Math.min(maximum, Integer.parseInt(string.trim())));
            } catch (NumberFormatException ignored) {
                // Use the documented default below.
            }
        }
        return fallback;
    }
}
