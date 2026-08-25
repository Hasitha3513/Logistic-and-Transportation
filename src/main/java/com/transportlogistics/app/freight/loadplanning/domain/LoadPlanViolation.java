package com.transportlogistics.app.freight.loadplanning.domain;

/**
 * Represents a structural load-plan violation detected during layout validation.
 * These are planning-level issues — NOT capacity/weight compliance (US-27).
 */
public record LoadPlanViolation(LoadPlanViolationCode code, String message) {
}
