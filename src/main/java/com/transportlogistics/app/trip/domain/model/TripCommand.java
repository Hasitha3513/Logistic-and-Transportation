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

    record Start(Double odometerKm) implements TripCommand {
    }

    record Complete(Double odometerKm, String remarks) implements TripCommand {
    }

    record Close() implements TripCommand {
    }

    record Cancel(String reason) implements TripCommand {
    }
}