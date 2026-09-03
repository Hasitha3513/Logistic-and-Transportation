package com.transportlogistics.app.notification.domain.model;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class NotificationDestination {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    private NotificationDestination() {
    }

    public static Optional<String> email(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return EMAIL.matcher(normalized).matches() ? Optional.of(normalized) : Optional.empty();
    }

    public static Optional<String> sms(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim().replace(" ", "").replace("-", "")
                .replace("(", "").replace(")", "");
        return E164.matcher(normalized).matches() ? Optional.of(normalized) : Optional.empty();
    }

    public static String mask(String recipient) {
        if (recipient == null || recipient.isBlank()) return null;
        int at = recipient.indexOf('@');
        if (at > 0) return recipient.substring(0, 1) + "***" + recipient.substring(at);
        if (recipient.startsWith("+") && recipient.length() > 6) {
            return recipient.substring(0, 3) + "***" + recipient.substring(recipient.length() - 2);
        }
        return recipient.length() < 3 ? "***" : recipient.substring(0, 2) + "***";
    }
}
