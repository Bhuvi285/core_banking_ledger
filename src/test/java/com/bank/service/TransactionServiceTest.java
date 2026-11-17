package com.bank.service;

import com.bank.TestDatabase;
import com.bank.dto.TransferRequest;
import com.bank.enums.AccountType;
import com.bank.exception.BankException;
import com.bank.exception.InsufficientFundsException;
import com.bank.model.LedgerEntry;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.LedgerRepository;
import com.bank.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL",
        matches = "^jdbc:postgresql://[^/?#]+/core_banking_ledger_test(?:\\?.*)?$")
class TransactionServiceTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void depositsAndWithdrawsAtomically() throws Exception {
        var account = services.accountService().createAccount("Test Customer", AccountType.SAVINGS);
        var deposit = services.transactionService().deposit(account.getAccountNumber(),
                new BigDecimal("100.00"), "Funding");
        assertEquals(new BigDecimal("100.00"),
                services.accountService().getAccount(account.getAccountNumber()).getBalance());
        assertTransactionArtifacts(deposit.getTransactionReference(), "DEPOSIT", "DEPOSIT_COMPLETED",
                new ExpectedLedgerEntry(account.getId(), "CREDIT", new BigDecimal("100.00")));

        var withdrawal = services.transactionService().withdraw(account.getAccountNumber(),
                new BigDecimal("35.50"), "Cash");

        assertEquals(new BigDecimal("64.50"),
                services.accountService().getAccount(account.getAccountNumber()).getBalance());
        assertTransactionArtifacts(withdrawal.getTransactionReference(), "WITHDRAWAL", "WITHDRAWAL_COMPLETED",
                new ExpectedLedgerEntry(account.getId(), "DEBIT", new BigDecimal("35.50")));
        assertEquals(2, countRows("transactions"));
    }

    @Test
    void transferCreatesBalancedDebitAndCreditAndInsufficientFundsRollsBack() throws Exception {
        var source = services.accountService().createAccount("Source Customer", AccountType.SAVINGS);
        var destination = services.accountService().createAccount("Destination Customer", AccountType.CURRENT);
        services.transactionService().deposit(source.getAccountNumber(), new BigDecimal("100.00"), "Funding");

        var result = services.transactionService().transfer(new TransferRequest(source.getAccountNumber(),
                destination.getAccountNumber(), new BigDecimal("40.00"), "Transfer"));
        assertEquals("SUCCESS", result.getTransactionStatus());
        assertEquals(new BigDecimal("60.00"),
                services.accountService().getAccount(source.getAccountNumber()).getBalance());
        assertEquals(new BigDecimal("40.00"),
                services.accountService().getAccount(destination.getAccountNumber()).getBalance());
        assertTransactionArtifacts(result.getTransactionReference(), "TRANSFER", "TRANSFER_COMPLETED",
                new ExpectedLedgerEntry(source.getId(), "DEBIT", new BigDecimal("40.00")),
                new ExpectedLedgerEntry(destination.getId(), "CREDIT", new BigDecimal("40.00")));
        assertEquals(new BigDecimal("0.00"),
                transferDebitCreditDifference(result.getTransactionReference()));

        RowCounts beforeInsufficientTransfer = rowCounts();
        assertThrows(InsufficientFundsException.class, () -> services.transactionService().transfer(
                new TransferRequest(source.getAccountNumber(), destination.getAccountNumber(),
                        new BigDecimal("500.00"), "Too large")));
        assertEquals(beforeInsufficientTransfer, rowCounts());
        assertEquals(new BigDecimal("60.00"),
                services.accountService().getAccount(source.getAccountNumber()).getBalance());
        assertEquals(new BigDecimal("40.00"),
                services.accountService().getAccount(destination.getAccountNumber()).getBalance());
    }

    @Test
    void depositFailureAfterTransactionBalanceAndLedgerWritesRollsEverythingBack() throws Exception {
        var account = services.accountService().createAccount("Deposit Rollback Customer", AccountType.SAVINGS);
        RowCounts before = rowCounts();
        TestDatabase.Services failingServices = servicesFailingAudit("DEPOSIT_COMPLETED");

        assertThrows(BankException.class, () -> failingServices.transactionService().deposit(
                account.getAccountNumber(), new BigDecimal("25.00"), "Injected failure"));

        assertEquals(new BigDecimal("0.00"),
                services.accountService().getAccount(account.getAccountNumber()).getBalance());
        assertEquals(before, rowCounts());
    }

    @Test
    void withdrawalFailureAfterTransactionBalanceAndLedgerWritesRollsEverythingBack() throws Exception {
        var account = services.accountService().createAccount("Withdrawal Rollback Customer", AccountType.SAVINGS);
        services.transactionService().deposit(account.getAccountNumber(), new BigDecimal("80.00"), "Funding");
        RowCounts before = rowCounts();
        TestDatabase.Services failingServices = servicesFailingAudit("WITHDRAWAL_COMPLETED");

        assertThrows(BankException.class, () -> failingServices.transactionService().withdraw(
                account.getAccountNumber(), new BigDecimal("30.00"), "Injected failure"));

        assertEquals(new BigDecimal("80.00"),
                services.accountService().getAccount(account.getAccountNumber()).getBalance());
        assertEquals(before, rowCounts());
    }

    @Test
    void insufficientFundsWithdrawalDoesNotCreateAnyRecords() throws Exception {
        var account = services.accountService().createAccount("Insufficient Funds Customer", AccountType.SAVINGS);
        RowCounts before = rowCounts();

        assertThrows(InsufficientFundsException.class, () -> services.transactionService().withdraw(
                account.getAccountNumber(), new BigDecimal("10.00"), "Too large"));

        assertEquals(new BigDecimal("0.00"),
                services.accountService().getAccount(account.getAccountNumber()).getBalance());
        assertEquals(before, rowCounts());
    }

    @Test
    void transferFailureAfterFirstLedgerWriteRollsBackBothBalancesAndAllRecords() throws Exception {
        var source = services.accountService().createAccount("Transfer Source", AccountType.SAVINGS);
        var destination = services.accountService().createAccount("Transfer Destination", AccountType.CURRENT);
        services.transactionService().deposit(source.getAccountNumber(), new BigDecimal("90.00"), "Funding");
        boolean failCredit = true;
        LedgerRepository failingLedgerRepository = new LedgerRepository() {
            @Override
            public LedgerEntry saveCredit(Connection connection, long transactionId, long accountId, BigDecimal amount)
                    throws SQLException {
                if (failCredit) {
                    throw new SQLException("Injected failure after the transfer debit ledger write.");
                }
                return super.saveCredit(connection, transactionId, accountId, amount);
            }
        };
        TestDatabase.Services failingServices = TestDatabase.services(new IdGenerator(),
                new AccountRepository(), failingLedgerRepository, new AuditRepository());
        RowCounts before = rowCounts();

        assertThrows(BankException.class, () -> failingServices.transactionService().transfer(
                new TransferRequest(source.getAccountNumber(), destination.getAccountNumber(),
                        new BigDecimal("25.00"), "Injected failure")));

        assertEquals(new BigDecimal("90.00"),
                services.accountService().getAccount(source.getAccountNumber()).getBalance());
        assertEquals(new BigDecimal("0.00"),
                services.accountService().getAccount(destination.getAccountNumber()).getBalance());
        assertEquals(before, rowCounts());
    }

    private static TestDatabase.Services servicesFailingAudit(String failingAction) {
        AuditRepository failingAuditRepository = new AuditRepository() {
            @Override
            public void save(Connection connection, String action, String entityType, long entityId,
                             String description) throws SQLException {
                if (failingAction.equals(action)) {
                    throw new SQLException("Injected failure after account, transaction, and ledger writes.");
                }
                super.save(connection, action, entityType, entityId, description);
            }
        };
        return TestDatabase.services(new IdGenerator(), new AccountRepository(), new LedgerRepository(),
                failingAuditRepository);
    }

    private static void assertTransactionArtifacts(String reference, String expectedType, String expectedAudit,
                                                   ExpectedLedgerEntry... expectedEntries) throws Exception {
        long transactionId;
        try (Connection connection = TestDatabase.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, transaction_type, transaction_status
                     FROM transactions WHERE transaction_reference = ?
                     """)) {
            statement.setString(1, reference);
            try (ResultSet results = statement.executeQuery()) {
                assertTrue(results.next(), "Transaction record should exist.");
                transactionId = results.getLong("id");
                assertEquals(expectedType, results.getString("transaction_type"));
                assertEquals("SUCCESS", results.getString("transaction_status"));
                assertFalse(results.next(), "Transaction reference must be unique.");
            }
        }

        List<ExpectedLedgerEntry> actualEntries = new ArrayList<>();
        try (Connection connection = TestDatabase.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT le.account_id, le.entry_type, le.amount, a.id AS valid_account_id,
                            t.id AS valid_transaction_id
                     FROM ledger_entries le
                     LEFT JOIN transactions t ON t.id = le.transaction_id
                     LEFT JOIN accounts a ON a.id = le.account_id
                     WHERE le.transaction_id = ?
                     ORDER BY le.entry_type, le.account_id
                     """)) {
            statement.setLong(1, transactionId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    assertEquals(transactionId, results.getLong("valid_transaction_id"),
                            "Ledger entry must reference an existing transaction.");
                    assertEquals(results.getLong("account_id"), results.getLong("valid_account_id"),
                            "Ledger entry must reference an existing account.");
                    BigDecimal amount = results.getBigDecimal("amount");
                    assertTrue(amount.signum() > 0, "Ledger amounts must be positive.");
                    actualEntries.add(new ExpectedLedgerEntry(results.getLong("account_id"),
                            results.getString("entry_type"), amount));
                }
            }
        }
        List<ExpectedLedgerEntry> sortedExpected = new ArrayList<>(List.of(expectedEntries));
        sortedExpected.sort((left, right) -> {
            int typeOrder = left.entryType().compareTo(right.entryType());
            return typeOrder != 0 ? typeOrder : Long.compare(left.accountId(), right.accountId());
        });
        assertEquals(sortedExpected, actualEntries);

        try (Connection connection = TestDatabase.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*) FROM audit_logs
                     WHERE entity_type = 'TRANSACTION' AND entity_id = ? AND action = ?
                     """)) {
            statement.setLong(1, transactionId);
            statement.setString(2, expectedAudit);
            try (ResultSet results = statement.executeQuery()) {
                assertTrue(results.next());
                assertEquals(1, results.getInt(1));
            }
        }
    }

    private static BigDecimal transferDebitCreditDifference(String reference) throws Exception {
        try (Connection connection = TestDatabase.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0)
                          - COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0)
                     FROM ledger_entries le
                     JOIN transactions t ON t.id = le.transaction_id
                     WHERE t.transaction_reference = ?
                     """)) {
            statement.setString(1, reference);
            try (ResultSet results = statement.executeQuery()) {
                assertTrue(results.next());
                return results.getBigDecimal(1);
            }
        }
    }

    private static RowCounts rowCounts() throws Exception {
        return new RowCounts(countRows("accounts"), countRows("transactions"), countRows("ledger_entries"),
                countRows("audit_logs"));
    }

    private static long countRows(String table) throws Exception {
        try (Connection connection = TestDatabase.connection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(results.next());
            return results.getLong(1);
        }
    }

    private record ExpectedLedgerEntry(long accountId, String entryType, BigDecimal amount) {
    }

    private record RowCounts(long accounts, long transactions, long ledgerEntries, long auditLogs) {
    }
}
