package com.bank.infrastructure;

import com.bank.exception.BankException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);
    private final ConnectionFactory connectionFactory;

    public TransactionManager(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public <T> T execute(TransactionWork<T> work) {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Exception exception) {
                rollback(connection, exception);
                if (exception instanceof BankException bankException) {
                    throw bankException;
                }
                if (exception instanceof SQLException sqlException) {
                    if ("23505".equals(sqlException.getSQLState())) {
                        throw new BankException("A record with the same unique value already exists.", sqlException);
                    }
                    throw new BankException("Database operation failed; the transaction was rolled back.", sqlException);
                }
                throw new BankException("Transaction failed and was rolled back.", exception);
            }
        } catch (SQLException exception) {
            throw new BankException("Unable to complete database transaction.", exception);
        }
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
            LOGGER.error("Database transaction rollback failed.", rollbackException);
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws Exception;
    }
}
