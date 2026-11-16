package com.bank.repository;

import com.bank.TestDatabase;
import com.bank.enums.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL",
        matches = "^jdbc:postgresql://[^/?#]+/core_banking_ledger_test(?:\\?.*)?$")
class AccountRepositoryTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void retrievesCreatedAccountByNumberAndListsAccounts() {
        var created = services.accountService().createAccount("Repository Customer", AccountType.CURRENT);
        assertEquals(created.getId(),
                services.accountService().getAccount(created.getAccountNumber()).getId());
        assertEquals(1, services.accountService().getAllAccounts().size());
    }

    @Test
    void databaseRejectsDuplicateAccountNumbers() throws Exception {
        try (var connection = TestDatabase.connection();
             var statement = connection.prepareStatement("""
                     INSERT INTO accounts (account_number, account_holder_name, account_type, balance, account_status)
                     VALUES ('123456789012', 'First', 'SAVINGS', 0.00, 'ACTIVE'),
                            ('123456789012', 'Second', 'SAVINGS', 0.00, 'ACTIVE')
                     """)) {
            assertThrows(java.sql.SQLException.class, statement::executeUpdate);
        }
    }
}
