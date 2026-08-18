package com.transportlogistics.app.shared.utils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static OffsetDateTime currentUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static boolean isChronologicallyOrdered(OffsetDateTime earlier, OffsetDateTime later) {
        if (earlier == null || later == null) {
            return false;
        }
        return !later.isBefore(earlier);
    }
}
