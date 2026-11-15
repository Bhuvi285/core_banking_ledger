package com.bank.integration;

import com.bank.TestDatabase;
import com.bank.enums.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL", matches = "jdbc:postgresql:.*_test($|\\?.*)")
class DatabaseIntegrationTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void transferPersistsEqualDebitAndCreditEntries() throws Exception {
        var source = services.accountService().createAccount("Source", AccountType.SAVINGS);
        var destination = services.accountService().createAccount("Destination", AccountType.CURRENT);
        services.transactionService().deposit(source.getAccountNumber(), new BigDecimal("75.00"), "Funding");
        var transfer = services.transactionService().transfer(new com.bank.dto.TransferRequest(
                source.getAccountNumber(), destination.getAccountNumber(), new BigDecimal("30.00"), "Integration"));

        try (var connection = TestDatabase.connection();
             var statement = connection.prepareStatement("""
                     SELECT entry_type, amount FROM ledger_entries le
                     JOIN transactions t ON t.id = le.transaction_id
                     WHERE t.transaction_reference = ? ORDER BY entry_type
                     """)) {
            statement.setString(1, transfer.getTransactionReference());
            try (var rows = statement.executeQuery()) {
                assertEquals(true, rows.next());
                assertEquals("CREDIT", rows.getString("entry_type"));
                assertEquals(new BigDecimal("30.00"), rows.getBigDecimal("amount"));
                assertEquals(true, rows.next());
                assertEquals("DEBIT", rows.getString("entry_type"));
                assertEquals(new BigDecimal("30.00"), rows.getBigDecimal("amount"));
                assertEquals(false, rows.next());
            }
        }
    }
}
