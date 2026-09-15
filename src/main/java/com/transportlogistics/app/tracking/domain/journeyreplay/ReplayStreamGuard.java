package com.transportlogistics.app.tracking.domain.journeyreplay;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;

import java.time.Instant;
import java.util.UUID;

public final class ReplayStreamGuard {
    private Instant snapshot;
    private CursorPosition last;
    private String previousCursor;
    private int count;

    public void accept(String requestedCursor, ReplayPage page) {
        if (snapshot == null) snapshot = page.snapshotRecordedAt();
        else if (!snapshot.equals(page.snapshotRecordedAt())) fail();
        if (requestedCursor != null && requestedCursor.equals(page.nextCursor())) fail();
        for (JourneyPoint point : page.items()) {
            if (last != null && compare(point.sourceTimestamp(), point.historyId(), last) <= 0) fail();
            last = new CursorPosition(point.sourceTimestamp(), point.historyId());
            count++;
            if (count > BROWSER_POINT_CEILING) {
                throw new JourneyReplayException(JourneyReplayError.REPLAY_POINT_LIMIT_EXCEEDED);
            }
        }
        if (page.nextCursor() != null && page.items().isEmpty()) fail();
        if (page.nextCursor() != null && page.nextCursor().equals(previousCursor)) fail();
        previousCursor = page.nextCursor();
    }

    public int count() { return count; }
    public Instant snapshot() { return snapshot; }

    private static int compare(Instant timestamp, UUID id, CursorPosition position) {
        int time = timestamp.compareTo(position.sourceTimestamp());
        return time == 0 ? id.compareTo(position.historyId()) : time;
    }

    private static void fail() {
        throw new JourneyReplayException(JourneyReplayError.REPLAY_CURSOR_NOT_ADVANCING);
    }
}
