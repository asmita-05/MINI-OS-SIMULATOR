package os.ipc;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Immutable IPC payload used by direct mailboxes and named channels.
 */
public record IPCMessage(
        long id,
        int fromPid,
        Integer toPid,
        String channel,
        String payload,
        long timestampMillis
) {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public boolean isDirect() {
        return toPid != null;
    }

    public String timestampLabel() {
        return TS_FMT.format(Instant.ofEpochMilli(timestampMillis));
    }
}
