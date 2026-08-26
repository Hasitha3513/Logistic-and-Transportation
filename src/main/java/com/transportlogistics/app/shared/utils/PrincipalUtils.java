package com.transportlogistics.app.shared.utils;

import java.security.Principal;

public final class PrincipalUtils {

    private PrincipalUtils() {
    }

    public static String resolveActorName(Principal principal, String defaultActor) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return defaultActor;
        }
        return principal.getName().trim();
    }

    public static String resolveActorName(Principal principal) {
        return resolveActorName(principal, "system");
    }
}
