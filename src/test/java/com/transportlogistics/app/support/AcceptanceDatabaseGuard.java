package com.transportlogistics.app.support;

public final class AcceptanceDatabaseGuard {

    public static final String REQUIRED_DATABASE = "transport_logistics_acceptance";

    private AcceptanceDatabaseGuard() {
    }

    static void requireConnectedDatabase(String metadataDatabase, String queriedDatabase) {
        if (!REQUIRED_DATABASE.equals(metadataDatabase)
                || !REQUIRED_DATABASE.equals(queriedDatabase)) {
            throw new IllegalStateException("Destructive PostgreSQL integration tests require connected database "
                    + REQUIRED_DATABASE);
        }
    }

    public static void verify(javax.sql.DataSource dataSource) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT current_database()")) {
            if (!result.next()) {
                throw new IllegalStateException("Cannot verify the connected PostgreSQL database");
            }
            requireConnectedDatabase(connection.getCatalog(), result.getString(1));
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Cannot verify the connected PostgreSQL database", exception);
        }
    }
}
