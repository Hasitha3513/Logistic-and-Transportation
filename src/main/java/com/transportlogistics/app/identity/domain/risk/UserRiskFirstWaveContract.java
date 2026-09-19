package com.transportlogistics.app.identity.domain.risk;

import java.time.Duration;

/** Frozen CS01 vocabulary and thresholds for the approved US-87 first wave. */
public final class UserRiskFirstWaveContract {
    public static final int DISTINCT_FACT_THRESHOLD = 3;
    public static final Duration EVALUATION_WINDOW = Duration.ofMinutes(15);
    public static final Duration DELIVERY_LATENESS = Duration.ofMinutes(5);
    public static final Duration RETENTION = Duration.ofDays(180);
    public static final Duration APPEAL_WINDOW = Duration.ofDays(30);

    private UserRiskFirstWaveContract() {
    }

    public enum SignalType {
        UNAUTHORIZED_OVERRIDE_ATTEMPT
    }

    public enum SourceModule {
        IDENTITY
    }

    public enum Action {
        IDENTITY_USER_CREATE_ROLE_PERMISSION_CEILING,
        IDENTITY_USER_UPDATE_ROLE_PERMISSION_CEILING,
        IDENTITY_ROLE_CREATE_PERMISSION_CEILING,
        IDENTITY_ROLE_UPDATE_PERMISSION_CEILING;

        public ActionFamily family() {
            return ActionFamily.IDENTITY_PERMISSION_CEILING;
        }
    }

    public enum ActionFamily {
        IDENTITY_PERMISSION_CEILING
    }

    public enum Reason {
        REQUESTED_PERMISSION_EXCEEDS_ACTOR_CEILING
    }

    public enum TargetType {
        USER,
        ROLE
    }

    public enum EvidenceState {
        SUFFICIENT,
        INSUFFICIENT,
        UNKNOWN,
        CONFLICTING,
        STALE;

        public boolean mayContributeToFinding() {
            return this == SUFFICIENT;
        }
    }

    public enum ReviewPriority {
        MEDIUM
    }

    public enum Effect {
        ADVISORY_REVIEW_ONLY
    }

    public enum FindingState {
        OPEN_REVIEW,
        DISPOSED,
        APPEALED
    }

    public enum ReviewDisposition {
        CONFIRMED_INDICATOR,
        FALSE_POSITIVE,
        INSUFFICIENT_EVIDENCE,
        CLOSED_NO_ACTION
    }

    public enum SignalComparison {
        DISTINCT,
        DUPLICATE,
        CONFLICTING
    }
}
