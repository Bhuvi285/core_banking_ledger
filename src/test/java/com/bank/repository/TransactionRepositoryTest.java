package com.bank.repository;

import com.bank.TestDatabase;
import com.bank.enums.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL", matches = "jdbc:postgresql:.*_test($|\\?.*)")
class TransactionRepositoryTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void retrievesSavedTransactionsInHistory() {
        var account = services.accountService().createAccount("History Customer", AccountType.SAVINGS);
        var result = services.transactionService().deposit(account.getAccountNumber(),
                new BigDecimal("25.00"), "History test");
        var history = services.statementService().getTransactionHistory(account.getAccountNumber(),
                OffsetDateTime.parse("2000-01-01T00:00:00Z"), OffsetDateTime.now());

        assertEquals(1, history.size());
        assertEquals(result.getTransactionReference(), history.get(0).getTransactionReference());
    }
}
