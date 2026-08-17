package com.transportlogistics.app.trip.domain.model;

public sealed interface TripCommand permits TripCommand.Submit, TripCommand.Approve, TripCommand.Reject, TripCommand.Dispatch, TripCommand.Start, TripCommand.Complete, TripCommand.Close, TripCommand.Cancel {
    record Submit() implements TripCommand {
    }

    record Approve() implements TripCommand {
    }

    record Reject(String reason) implements TripCommand {
    }

    record Dispatch() implements TripCommand {
    }

    record Start(Double odometerKm, Double engineHours) implements TripCommand {
        public Start(Double odometerKm) {
            this(odometerKm, null);
        }
    }

    record Complete(Double odometerKm, String remarks, Double engineHours) implements TripCommand {
        public Complete(Double odometerKm, String remarks) {
            this(odometerKm, remarks, null);
        }
    }

    record Close() implements TripCommand {
    }

    record Cancel(String reason) implements TripCommand {
    }
}