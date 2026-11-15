package com.bank.repository;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountRepository {
    private static final String ACCOUNT_COLUMNS = """
            id, account_number, account_holder_name, account_type, balance, account_status, created_at, updated_at
            """;

    public Account create(Connection connection, Account account) throws SQLException {
        String sql = """
                INSERT INTO accounts (account_number, account_holder_name, account_type, balance, account_status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, account.getAccountNumber());
            statement.setString(2, account.getAccountHolderName());
            statement.setString(3, account.getAccountType().name());
            statement.setBigDecimal(4, account.getBalance());
            statement.setString(5, account.getAccountStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Account insert did not return a generated key.");
                }
                account.setId(keys.getLong(1));
            }
        }
        return findById(connection, account.getId()).orElseThrow(
                () -> new SQLException("Created account could not be read back."));
    }

    public Optional<Account> findByAccountNumber(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapAccount(results)) : Optional.empty();
            }
        }
    }

    public Optional<Account> findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapAccount(results)) : Optional.empty();
            }
        }
    }

    public List<Account> findByAccountNumbersForUpdate(Connection connection, String firstAccountNumber,
                                                        String secondAccountNumber) throws SQLException {
        String sql = "SELECT " + ACCOUNT_COLUMNS
                + " FROM accounts WHERE account_number IN (?, ?) ORDER BY id FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstAccountNumber);
            statement.setString(2, secondAccountNumber);
            try (ResultSet results = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (results.next()) {
                    accounts.add(mapAccount(results));
                }
                return accounts;
            }
        }
    }

    public Optional<Account> findByAccountNumberForUpdate(Connection connection, String accountNumber)
            throws SQLException {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts WHERE account_number = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? Optional.of(mapAccount(results)) : Optional.empty();
            }
        }
    }

    public void updateBalance(Connection connection, long accountId, java.math.BigDecimal balance)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setBigDecimal(1, balance);
            statement.setLong(2, accountId);
            requireOneUpdatedRow(statement.executeUpdate(), "Account balance could not be updated.");
        }
    }

    public void updateBalance(Connection connection, long accountId, java.math.BigDecimal balance,
                              java.math.BigDecimal minimumBalance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND balance >= ?
                """)) {
            statement.setBigDecimal(1, balance);
            statement.setLong(2, accountId);
            statement.setBigDecimal(3, minimumBalance);
            requireOneUpdatedRow(statement.executeUpdate(), "Account balance could not be updated.");
        }
    }

    public void updateStatus(Connection connection, long accountId, AccountStatus status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setString(1, status.name());
            statement.setLong(2, accountId);
            requireOneUpdatedRow(statement.executeUpdate(), "Account status could not be updated.");
        }
    }

    public List<Account> findAll(Connection connection) throws SQLException {
        String sql = "SELECT " + ACCOUNT_COLUMNS + " FROM accounts ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<Account> accounts = new ArrayList<>();
            while (results.next()) {
                accounts.add(mapAccount(results));
            }
            return accounts;
        }
    }

    private static Account mapAccount(ResultSet result) throws SQLException {
        return new Account(
                result.getLong("id"),
                result.getString("account_number"),
                result.getString("account_holder_name"),
                AccountType.valueOf(result.getString("account_type")),
                result.getBigDecimal("balance"),
                AccountStatus.valueOf(result.getString("account_status")),
                result.getObject("created_at", OffsetDateTime.class),
                result.getObject("updated_at", OffsetDateTime.class));
    }

    private static void requireOneUpdatedRow(int count, String message) throws SQLException {
        if (count != 1) {
            throw new SQLException(message);
        }
    }
}
