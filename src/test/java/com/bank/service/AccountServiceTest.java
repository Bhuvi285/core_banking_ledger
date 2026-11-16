package com.bank.service;

import com.bank.TestDatabase;
import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.exception.BankException;
import com.bank.util.AccountNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "DB_TEST_URL", matches = "jdbc:postgresql:.*_test($|\\?.*)")
class AccountServiceTest {
    private TestDatabase.Services services;

    @BeforeEach
    void setUp() throws Exception {
        TestDatabase.reset();
        services = TestDatabase.services();
    }

    @Test
    void createsActiveAccountWithZeroBalance() throws Exception {
        var account = services.accountService().createAccount("Test Customer", AccountType.SAVINGS);
        assertTrue(account.getAccountNumber().matches("9\\d{11}"));
        assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
        assertEquals("0.00", account.getBalance().toPlainString());
        assertEquals(account.getId(), services.accountService().getAccount(account.getAccountNumber()).getId());
        assertEquals(1, accountCount());
        assertEquals(1, creationAuditCount());
    }

    @Test
    void retriesAccountNumberCollisionAndCreatesOneAccountAndAudit() throws Exception {
        String collisionNumber = "912345678901";
        String availableNumber = "998765432109";
        insertAccount(collisionNumber, "Existing Customer");
        long accountsBefore = accountCount();
        long auditsBefore = creationAuditCount();
        AtomicInteger generatorCalls = new AtomicInteger();
        List<String> candidates = List.of(collisionNumber, availableNumber);
        AccountNumberGenerator generator = () -> candidates.get(generatorCalls.getAndIncrement());
        var retryServices = TestDatabase.services(generator);

        var created = retryServices.accountService().createAccount("New Customer", AccountType.SAVINGS);

        assertEquals(availableNumber, created.getAccountNumber());
        assertEquals(2, generatorCalls.get());
        assertEquals(accountsBefore + 1, accountCount());
        assertEquals(auditsBefore + 1, creationAuditCount());
    }

    @Test
    void stopsAfterThreeAccountNumberCollisionsWithoutWritingAccountOrAudit() throws Exception {
        List<String> candidates = List.of("912345678901", "912345678902", "912345678903");
        for (int index = 0; index < candidates.size(); index++) {
            insertAccount(candidates.get(index), "Existing Customer " + index);
        }
        long accountsBefore = accountCount();
        long auditsBefore = creationAuditCount();
        AtomicInteger generatorCalls = new AtomicInteger();
        AccountNumberGenerator generator = () -> candidates.get(generatorCalls.getAndIncrement());
        var retryServices = TestDatabase.services(generator);

        assertThrows(BankException.class,
                () -> retryServices.accountService().createAccount("New Customer", AccountType.SAVINGS));

        assertEquals(3, generatorCalls.get());
        assertEquals(accountsBefore, accountCount());
        assertEquals(auditsBefore, creationAuditCount());
    }

    @Test
    void doesNotRetryAnUnrelatedUniqueConstraint() throws Exception {
        String constraintName = "account_holder_name_test_unique";
        AtomicInteger generatorCalls = new AtomicInteger();
        AccountNumberGenerator generator = () -> {
            generatorCalls.incrementAndGet();
            return "923456789012";
        };
        var retryServices = TestDatabase.services(generator);
        try (Connection connection = TestDatabase.connection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE accounts ADD CONSTRAINT " + constraintName
                    + " UNIQUE (account_holder_name)");
            insertAccount("934567890123", "Duplicate Holder");
            long accountsBefore = accountCount();
            long auditsBefore = creationAuditCount();

            assertThrows(BankException.class, () -> retryServices.accountService()
                    .createAccount("Duplicate Holder", AccountType.SAVINGS));

            assertEquals(1, generatorCalls.get());
            assertEquals(accountsBefore, accountCount());
            assertEquals(auditsBefore, creationAuditCount());
        } finally {
            try (Connection connection = TestDatabase.connection(); Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE accounts DROP CONSTRAINT IF EXISTS " + constraintName);
            }
        }
    }

    @Test
    void invalidInputDoesNotConsumeAccountNumberGenerator() throws Exception {
        AtomicInteger generatorCalls = new AtomicInteger();
        AccountNumberGenerator generator = () -> {
            generatorCalls.incrementAndGet();
            return "912345678901";
        };
        var retryServices = TestDatabase.services(generator);

        assertThrows(IllegalArgumentException.class,
                () -> retryServices.accountService().createAccount(" ", AccountType.SAVINGS));
        assertThrows(IllegalArgumentException.class,
                () -> retryServices.accountService().createAccount("Valid Customer", null));
        assertEquals(0, generatorCalls.get());
        assertEquals(0, accountCount());
        assertEquals(0, creationAuditCount());
    }

    @Test
    void rejectsTransactionsWhenAccountIsFrozen() {
        var account = services.accountService().createAccount("Test Customer", AccountType.SAVINGS);
        services.accountService().freezeAccount(account.getAccountNumber());
        assertThrows(BankException.class, () -> services.transactionService()
                .deposit(account.getAccountNumber(), new java.math.BigDecimal("10.00"), "test"));
    }

    private static void insertAccount(String accountNumber, String holder) throws Exception {
        try (Connection connection = TestDatabase.connection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO accounts (account_number, account_holder_name, account_type, balance, account_status)
                     VALUES (?, ?, 'SAVINGS', 0.00, 'ACTIVE')
                     """)) {
            statement.setString(1, accountNumber);
            statement.setString(2, holder);
            statement.executeUpdate();
        }
    }

    private static long accountCount() throws Exception {
        return count("SELECT COUNT(*) FROM accounts");
    }

    private static long creationAuditCount() throws Exception {
        return count("SELECT COUNT(*) FROM audit_logs WHERE action = 'ACCOUNT_CREATED'");
    }

    private static long count(String sql) throws Exception {
        try (Connection connection = TestDatabase.connection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            results.next();
            return results.getLong(1);
        }
    }
}
