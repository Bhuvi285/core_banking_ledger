package com.bank.repository;

import com.bank.enums.TransactionType;
import com.bank.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionRepository {
    private static final String TRANSACTION_COLUMNS = """
            id, transaction_reference, transaction_type, source_account_id, destination_account_id,
            amount, transaction_status, description, created_at
            """;

    public Transaction save(Connection connection, Transaction transaction) throws SQLException {
        String sql = """
                INSERT INTO transactions
                    (transaction_reference, transaction_type, source_account_id, destination_account_id,
                     amount, transaction_status, description)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transaction.getTransactionReference());
            statement.setString(2, transaction.getTransactionType().name());
            if (transaction.getSourceAccountId() == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, transaction.getSourceAccountId());
            }
            if (transaction.getDestinationAccountId() == null) {
                statement.setNull(4, java.sql.Types.BIGINT);
            } else {
                statement.setLong(4, transaction.getDestinationAccountId());
            }
            statement.setBigDecimal(5, transaction.getAmount());
            statement.setString(6, transaction.getTransactionStatus());
            statement.setString(7, transaction.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Transaction insert did not return a generated key.");
                }
                transaction.setId(keys.getLong(1));
            }
        }
        return findByReference(connection, transaction.getTransactionReference()).orElseThrow(
                () -> new SQLException("Created transaction could not be read back."));
    }

    public Optional<Transaction> findByReference(Connection connection, String reference) throws SQLException {
        String sql = "SELECT " + TRANSACTION_COLUMNS + " FROM transactions WHERE transaction_reference = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reference);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapTransaction(results)) : Optional.empty();
            }
        }
    }

    public List<Transaction> findHistory(Connection connection, long accountId, OffsetDateTime from,
                                         OffsetDateTime to) throws SQLException {
        String sql = """
                SELECT %s FROM transactions
                WHERE (source_account_id = ? OR destination_account_id = ?)
                  AND created_at >= ? AND created_at <= ?
                ORDER BY created_at DESC, id DESC
                """.formatted(TRANSACTION_COLUMNS);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setLong(2, accountId);
            statement.setObject(3, from);
            statement.setObject(4, to);
            try (ResultSet results = statement.executeQuery()) {
                List<Transaction> transactions = new ArrayList<>();
                while (results.next()) {
                    transactions.add(mapTransaction(results));
                }
                return transactions;
            }
        }
    }

    private static Transaction mapTransaction(ResultSet result) throws SQLException {
        long source = result.getLong("source_account_id");
        Long sourceId = result.wasNull() ? null : source;
        long destination = result.getLong("destination_account_id");
        Long destinationId = result.wasNull() ? null : destination;
        return new Transaction(
                result.getLong("id"),
                result.getString("transaction_reference"),
                TransactionType.valueOf(result.getString("transaction_type")),
                sourceId,
                destinationId,
                result.getBigDecimal("amount"),
                result.getString("transaction_status"),
                result.getString("description"),
                result.getObject("created_at", OffsetDateTime.class));
    }
}
