package com.bank.repository;

import com.bank.model.LedgerEntry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class LedgerRepository {
    public LedgerEntry saveDebit(Connection connection, long transactionId, long accountId, BigDecimal amount)
            throws SQLException {
        return save(connection, transactionId, accountId, "DEBIT", amount);
    }

    public LedgerEntry saveCredit(Connection connection, long transactionId, long accountId, BigDecimal amount)
            throws SQLException {
        return save(connection, transactionId, accountId, "CREDIT", amount);
    }

    private LedgerEntry save(Connection connection, long transactionId, long accountId, String entryType,
                             BigDecimal amount) throws SQLException {
        String sql = """
                INSERT INTO ledger_entries (transaction_id, account_id, entry_type, amount)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, accountId);
            statement.setString(3, entryType);
            statement.setBigDecimal(4, amount);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Ledger entry insert did not return a generated key.");
                }
                return new LedgerEntry(keys.getLong(1), transactionId, accountId, entryType, amount,
                        OffsetDateTime.now());
            }
        }
    }

    public List<LedgerEntry> findByAccount(Connection connection, long accountId, OffsetDateTime from,
                                           OffsetDateTime to) throws SQLException {
        String sql = """
                SELECT id, transaction_id, account_id, entry_type, amount, created_at
                FROM ledger_entries
                WHERE account_id = ? AND created_at >= ? AND created_at <= ?
                ORDER BY created_at, id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setObject(2, from);
            statement.setObject(3, to);
            try (ResultSet results = statement.executeQuery()) {
                List<LedgerEntry> entries = new ArrayList<>();
                while (results.next()) {
                    entries.add(new LedgerEntry(
                            results.getLong("id"),
                            results.getLong("transaction_id"),
                            results.getLong("account_id"),
                            results.getString("entry_type"),
                            results.getBigDecimal("amount"),
                            results.getObject("created_at", OffsetDateTime.class)));
                }
                return entries;
            }
        }
    }

    public BigDecimal calculateTotal(Connection connection, long accountId, String entryType,
                                     OffsetDateTime before) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0.00) FROM ledger_entries
                WHERE account_id = ? AND entry_type = ? AND created_at < ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, entryType);
            statement.setObject(3, before);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getBigDecimal(1);
            }
        }
    }
}
