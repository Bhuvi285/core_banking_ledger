package com.bank.service;

import com.bank.TestDatabase;
import com.bank.enums.AccountType;
import com.bank.exception.BankException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL", matches = "jdbc:postgresql:.*_test($|\\?.*)")
class AccountServiceTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void createsActiveAccountWithZeroBalance() {
        var account = services.accountService().createAccount("Test Customer", AccountType.SAVINGS);
        assertEquals("0.00", account.getBalance().toPlainString());
        assertEquals(AccountType.SAVINGS, services.accountService()
                .getAccount(account.getAccountNumber()).getAccountType());
    }

    @Test
    void rejectsTransactionsWhenAccountIsFrozen() {
        var account = services.accountService().createAccount("Test Customer", AccountType.SAVINGS);
        services.accountService().freezeAccount(account.getAccountNumber());
        assertThrows(BankException.class, () -> services.transactionService()
                .deposit(account.getAccountNumber(), new java.math.BigDecimal("10.00"), "test"));
    }
}
