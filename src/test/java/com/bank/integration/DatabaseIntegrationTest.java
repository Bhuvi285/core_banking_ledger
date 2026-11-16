package com.bank.integration;

import com.bank.TestDatabase;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "DB_TEST_URL",
            matches = "^jdbc:postgresql://[^/?#]+/core_banking_ledger_test(?:\\?.*)?$")
    void connectsOnlyToCoreBankingLedgerTest() throws SQLException {
        Assumptions.assumeTrue(isConfigured("DB_TEST_USERNAME") && isConfigured("DB_TEST_PASSWORD"),
                "Dedicated PostgreSQL test credentials are not configured.");
        try (var connection = TestDatabase.connection()) {
            assertTrue(connection.isValid(5), "PostgreSQL connection should be valid.");

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
                assertTrue(resultSet.next(), "Database query should return a row.");
                assertEquals("core_banking_ledger_test", resultSet.getString(1));
                assertFalse(resultSet.next(), "Database name query should return exactly one row.");
            }
        }
    }

    private static boolean isConfigured(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    @Test
    void acceptsTheDedicatedTestDatabaseUrl() {
        assertDoesNotThrow(() -> TestDatabase.validateTestDatabaseUrl(
                "jdbc:postgresql://localhost:5432/core_banking_ledger_test"));
    }

    @Test
    void rejectsTheProductionDatabaseUrl() {
        assertThrows(IllegalStateException.class, () -> TestDatabase.validateTestDatabaseUrl(
                "jdbc:postgresql://localhost:5432/core_banking_ledger"));
    }

    @Test
    void rejectsTestNameOnlyInQueryParameters() {
        assertThrows(IllegalStateException.class, () -> TestDatabase.validateTestDatabaseUrl(
                "jdbc:postgresql://localhost:5432/core_banking_ledger?ApplicationName=core_banking_ledger_test"));
    }

    @Test
    void rejectsMalformedDatabaseUrl() {
        assertThrows(IllegalStateException.class,
                () -> TestDatabase.validateTestDatabaseUrl("jdbc:postgresql://localhost:not-a-port"));
    }
}
