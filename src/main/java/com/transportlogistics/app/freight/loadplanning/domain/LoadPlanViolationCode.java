package com.transportlogistics.app.freight.loadplanning.domain;

/**
 * Enumeration of structural load-plan violation codes.
 * These represent planning-level issues — NOT capacity/weight compliance (US-27).
 */
public enum LoadPlanViolationCode {

    ITEM_NOT_PLACED,
    DUPLICATE_PLACEMENT,
    STACKING_CONFLICT,
    COMPATIBILITY_CONFLICT,
    FRAGILE_SEPARATION_REQUIRED,
    TEMPERATURE_SEPARATION_REQUIRED,
    INVALID_LOADING_SEQUENCE
}
