package com.bank;

import com.bank.config.DatabaseConfig;
import com.bank.infrastructure.ConnectionFactory;
import com.bank.infrastructure.TransactionManager;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.LedgerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.AccountService;
import com.bank.service.StatementService;
import com.bank.service.TransactionService;
import com.bank.util.AccountNumberGenerator;
import com.bank.util.IdGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestDatabase {
    private TestDatabase() {
    }

    public static Connection connection() throws SQLException {
        return DriverManager.getConnection(required("DB_TEST_URL"), required("DB_TEST_USERNAME"),
                required("DB_TEST_PASSWORD"));
    }

    public static Services services() {
        return services(new IdGenerator());
    }

    public static Services services(AccountNumberGenerator accountNumberGenerator) {
        ConnectionFactory connectionFactory = new ConnectionFactory(DatabaseConfig.fromValues(
                required("DB_TEST_URL"), required("DB_TEST_USERNAME"), required("DB_TEST_PASSWORD")));
        TransactionManager transactionManager = new TransactionManager(connectionFactory);
        AccountRepository accountRepository = new AccountRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        LedgerRepository ledgerRepository = new LedgerRepository();
        AuditRepository auditRepository = new AuditRepository();
        AccountService accountService = new AccountService(transactionManager, accountRepository, auditRepository,
                accountNumberGenerator);
        TransactionService transactionService = new TransactionService(transactionManager, accountRepository,
                transactionRepository, ledgerRepository, auditRepository, accountService);
        StatementService statementService = new StatementService(transactionManager, accountRepository,
                transactionRepository, ledgerRepository);
        return new Services(accountRepository, transactionRepository, ledgerRepository, accountService,
                transactionService, statementService);
    }

    public static void reset() throws SQLException, IOException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            String schema;
            try (InputStream input = TestDatabase.class.getClassLoader().getResourceAsStream("schema.sql")) {
                if (input == null) {
                    throw new IOException("schema.sql was not found on the test classpath.");
                }
                schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (String command : schema.split(";")) {
                if (!command.isBlank()) {
                    statement.execute(command.trim());
                }
            }
            statement.execute("TRUNCATE TABLE audit_logs, ledger_entries, transactions, accounts "
                    + "RESTART IDENTITY CASCADE");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for PostgreSQL integration tests.");
        }
        return value;
    }

    public record Services(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           LedgerRepository ledgerRepository, AccountService accountService,
                           TransactionService transactionService, StatementService statementService) {
    }
}
