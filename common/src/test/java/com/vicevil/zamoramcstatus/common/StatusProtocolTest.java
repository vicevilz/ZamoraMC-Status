package com.vicevil.zamoramcstatus.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatusProtocolTest {
    @Test
    void requestRoundTripIsRecognized() {
        assertTrue(StatusProtocol.isRequest(StatusProtocol.request()));
        assertFalse(StatusProtocol.isRequest(new byte[] {1, 2, 3}));
    }

    @Test
    void snapshotRoundTripPreservesStatuses() {
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("lobby", true);
        expected.put("survival121", false);

        Map<String, Boolean> actual = StatusProtocol.decodeSnapshot(StatusProtocol.snapshot(expected)).orElseThrow();

        assertEquals(expected, actual);
    }

    @Test
    void malformedSnapshotIsRejected() {
        byte[] payload = StatusProtocol.snapshot(Map.of("lobby", true));
        byte[] malformed = java.util.Arrays.copyOf(payload, payload.length + 1);
        malformed[malformed.length - 1] = 7;
        assertTrue(StatusProtocol.decodeSnapshot(malformed).isEmpty());
    }
}
