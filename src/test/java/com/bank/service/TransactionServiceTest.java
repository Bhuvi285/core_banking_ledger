package com.bank.service;

import com.bank.TestDatabase;
import com.bank.dto.TransferRequest;
import com.bank.enums.AccountType;
import com.bank.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL", matches = "jdbc:postgresql:.*_test($|\\?.*)")
class TransactionServiceTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void depositsAndWithdrawsAtomically() {
        var account = services.accountService().createAccount("Test Customer", AccountType.SAVINGS);
        services.transactionService().deposit(account.getAccountNumber(), new BigDecimal("100.00"), "Funding");
        services.transactionService().withdraw(account.getAccountNumber(), new BigDecimal("35.50"), "Cash");

        assertEquals(new BigDecimal("64.50"),
                services.accountService().getAccount(account.getAccountNumber()).getBalance());
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

        assertThrows(InsufficientFundsException.class, () -> services.transactionService().transfer(
                new TransferRequest(source.getAccountNumber(), destination.getAccountNumber(),
                        new BigDecimal("500.00"), "Too large")));
        assertEquals(new BigDecimal("60.00"),
                services.accountService().getAccount(source.getAccountNumber()).getBalance());
        assertEquals(new BigDecimal("40.00"),
                services.accountService().getAccount(destination.getAccountNumber()).getBalance());

        try (var connection = TestDatabase.connection();
             var statement = connection.prepareStatement("""
                     SELECT COUNT(*) FROM ledger_entries le JOIN transactions t ON t.id = le.transaction_id
                     WHERE t.transaction_reference = ?
                     """)) {
            statement.setString(1, result.getTransactionReference());
            try (var rows = statement.executeQuery()) {
                rows.next();
                assertEquals(2, rows.getInt(1));
            }
        }
    }
}
