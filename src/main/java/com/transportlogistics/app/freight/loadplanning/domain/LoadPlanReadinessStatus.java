package com.transportlogistics.app.freight.loadplanning.domain;

/**
 * Structural readiness lifecycle status for a Load Plan.
 *
 * <p>A Load Plan starts as {@link #DRAFT}. It can only become {@link #STRUCTURALLY_READY}
 * via an explicit ready command after passing all mandatory US-26 structural checks.</p>
 *
 * <p>Material plan mutations or authoritative input changes return the plan to {@link #DRAFT}.</p>
 */
public enum LoadPlanReadinessStatus {
    DRAFT,
    STRUCTURALLY_READY
}
