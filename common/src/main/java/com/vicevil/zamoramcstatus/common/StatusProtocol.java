package com.vicevil.zamoramcstatus.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Wire format shared by the Velocity and Paper modules. */
public final class StatusProtocol {
    public static final String CHANNEL = "zamoramc:status";
    private static final int MAGIC = 0x5A4D5331; // ZMS1
    private static final byte REQUEST = 1;
    private static final byte SNAPSHOT = 2;
    private static final int MAX_ENTRIES = 1_000;

    private StatusProtocol() {
    }

    public static byte[] request() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(5);
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(MAGIC);
            output.writeByte(REQUEST);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode status request", exception);
        }
    }

    public static boolean isRequest(byte[] payload) {
        if (payload == null || payload.length != 5) {
            return false;
        }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return input.readInt() == MAGIC && input.readByte() == REQUEST;
        } catch (IOException exception) {
            return false;
        }
    }

    public static byte[] snapshot(Map<String, Boolean> statuses) {
        try {
            Map<String, Boolean> encodable = statuses.entrySet().stream()
                    .filter(entry -> entry.getKey() != null
                            && !entry.getKey().isBlank()
                            && entry.getKey().getBytes(StandardCharsets.UTF_8).length <= 255)
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (left, right) -> right,
                            LinkedHashMap::new));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(MAGIC);
            output.writeByte(SNAPSHOT);
            output.writeInt(encodable.size());
            for (Map.Entry<String, Boolean> entry : encodable.entrySet()) {
                byte[] name = entry.getKey().getBytes(StandardCharsets.UTF_8);
                output.writeByte(name.length);
                output.write(name);
                output.writeBoolean(Boolean.TRUE.equals(entry.getValue()));
            }
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode status snapshot", exception);
        }
    }

    public static Optional<Map<String, Boolean>> decodeSnapshot(byte[] payload) {
        if (payload == null || payload.length < 9) {
            return Optional.empty();
        }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            if (input.readInt() != MAGIC || input.readByte() != SNAPSHOT) {
                return Optional.empty();
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_ENTRIES) {
                return Optional.empty();
            }
            Map<String, Boolean> statuses = new LinkedHashMap<>();
            for (int index = 0; index < count; index++) {
                int length = input.readUnsignedByte();
                if (length == 0 || length > 255) {
                    return Optional.empty();
                }
                byte[] name = input.readNBytes(length);
                if (name.length != length) {
                    return Optional.empty();
                }
                statuses.put(new String(name, StandardCharsets.UTF_8), input.readBoolean());
            }
            if (input.available() != 0) {
                return Optional.empty();
            }
            return Optional.of(Map.copyOf(statuses));
        } catch (EOFException exception) {
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }
}
