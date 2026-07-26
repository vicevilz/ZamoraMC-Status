package com.vicevil.zamoramcstatus.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VelocityConfigTest {
    @Test
    void defaultConfigIsCreatedAndLoaded(@TempDir Path directory) throws Exception {
        VelocityConfig config = VelocityConfig.load(directory);

        assertEquals(5, config.checkIntervalSeconds());
        assertEquals(2_000, config.pingTimeoutMilliseconds());
        assertEquals("check-interval-seconds: 5\nping-timeout-milliseconds: 2000\n",
                Files.readString(directory.resolve("config.yml")));
    }

    @Test
    void numericValuesAreClamped(@TempDir Path directory) throws Exception {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("config.yml"),
                "check-interval-seconds: 0\nping-timeout-milliseconds: 999999\n");

        VelocityConfig config = VelocityConfig.load(directory);

        assertEquals(1, config.checkIntervalSeconds());
        assertEquals(30_000, config.pingTimeoutMilliseconds());
    }
}
