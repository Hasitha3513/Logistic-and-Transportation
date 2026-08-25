package com.transportlogistics.app.freight.loadplanning.domain;

/**
 * Details of a weight, volume, or axle capacity violation.
 */
public record LoadValidationViolation(
        String code,
        String message
) {
}
