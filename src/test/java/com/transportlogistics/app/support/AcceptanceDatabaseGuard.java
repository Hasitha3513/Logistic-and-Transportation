package com.transportlogistics.app.support;

final class AcceptanceDatabaseGuard {

    static final String REQUIRED_DATABASE = "transport_logistics_acceptance";

    private AcceptanceDatabaseGuard() {
    }

    static void requireConnectedDatabase(String metadataDatabase, String queriedDatabase) {
        if (!REQUIRED_DATABASE.equals(metadataDatabase)
                || !REQUIRED_DATABASE.equals(queriedDatabase)) {
            throw new IllegalStateException("Destructive PostgreSQL integration tests require connected database "
                    + REQUIRED_DATABASE);
        }
    }
}
