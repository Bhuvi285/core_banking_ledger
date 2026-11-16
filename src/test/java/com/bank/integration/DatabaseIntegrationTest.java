package com.bank.integration;

import com.bank.config.DatabaseConfig;
import com.bank.infrastructure.ConnectionFactory;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseIntegrationTest {
    @Test
    void connectsToCoreBankingLedger() throws SQLException {
        ConnectionFactory connectionFactory = new ConnectionFactory(DatabaseConfig.load());

        try (var connection = connectionFactory.getConnection()) {
            assertTrue(connection.isValid(5), "PostgreSQL connection should be valid.");

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
                assertTrue(resultSet.next(), "Database query should return a row.");
                assertEquals("core_banking_ledger", resultSet.getString(1));
                assertFalse(resultSet.next(), "Database name query should return exactly one row.");
            }
        }
    }
}
