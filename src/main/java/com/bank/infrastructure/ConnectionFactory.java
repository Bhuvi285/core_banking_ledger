package com.bank.infrastructure;

import com.bank.config.DatabaseConfig;
import com.bank.exception.BankException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private final DatabaseConfig config;

    public ConnectionFactory(DatabaseConfig config) {
        this.config = config;
        try {
            Class.forName(config.getDriver());
        } catch (ClassNotFoundException exception) {
            throw new BankException("PostgreSQL JDBC driver is unavailable.", exception);
        }
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
        } catch (SQLException exception) {
            throw new BankException("Unable to connect to the banking database. Verify the database configuration.",
                    exception);
        }
    }
}
