package com.transportlogistics.app.compliance.ports.outbound;

import com.transportlogistics.app.compliance.domain.ComplianceSourceFactReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface DriverEligibilityFactsQuery {

    DriverEligibilityFacts findAt(UUID tenantId, UUID driverId, Instant effectiveAt);

    record DriverEligibilityFacts(
            UUID tenantId,
            UUID driverId,
            List<LicenceFact> licences,
            List<FitnessFact> fitnessFacts) {

        public DriverEligibilityFacts {
            Objects.requireNonNull(tenantId, "tenantId is required");
            Objects.requireNonNull(driverId, "driverId is required");
            licences = List.copyOf(licences);
            fitnessFacts = List.copyOf(fitnessFacts);
            licences.forEach(fact -> requireTenant(tenantId, fact.reference()));
            fitnessFacts.forEach(fact -> requireTenant(tenantId, fact.reference()));
        }

        private static void requireTenant(
                UUID tenantId, ComplianceSourceFactReference reference) {
            if (!tenantId.equals(reference.tenantId())) {
                throw new IllegalArgumentException("Source fact must belong to the requested Tenant");
            }
        }
    }

    record LicenceFact(
            ComplianceSourceFactReference reference,
            String licenceClass,
            String lifecycleStatus,
            LocalDate validFrom,
            LocalDate validTo) {

        public LicenceFact {
            Objects.requireNonNull(reference, "reference is required");
            Objects.requireNonNull(licenceClass, "licenceClass is required");
            Objects.requireNonNull(lifecycleStatus, "lifecycleStatus is required");
        }
    }

    /** Contains only the minimized fitness outcome, never diagnosis or clinical detail. */
    record FitnessFact(
            ComplianceSourceFactReference reference,
            String fitnessOutcome,
            LocalDate validFrom,
            LocalDate validTo) {

        public FitnessFact {
            Objects.requireNonNull(reference, "reference is required");
            Objects.requireNonNull(fitnessOutcome, "fitnessOutcome is required");
        }
    }
}
