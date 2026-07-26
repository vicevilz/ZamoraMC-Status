package com.vicevil.zamoramcstatus.paper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StatusCacheTest {
    @Test
    void emptyCacheUsesFalseFallback() {
        StatusCache cache = new StatusCache();

        assertFalse(cache.get("survival121", false));
    }

    @Test
    void namesAreCaseInsensitiveAndSnapshotIsReplaced() {
        StatusCache cache = new StatusCache();
        cache.replace(Map.of("Survival121", true));

        assertTrue(cache.get("SURVIVAL121", false));
        assertFalse(cache.get("lobby", false));

        cache.clear();
        assertFalse(cache.get("SURVIVAL121", false));
    }
}
