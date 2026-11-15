package com.bank;

import com.bank.config.DatabaseConfig;
import com.bank.controller.AccountController;
import com.bank.controller.TransactionController;
import com.bank.infrastructure.ConnectionFactory;
import com.bank.infrastructure.TransactionManager;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.LedgerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.AccountService;
import com.bank.service.StatementService;
import com.bank.service.TransactionService;
import com.bank.util.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        try {
            run();
        } catch (RuntimeException exception) {
            LOGGER.error("Application could not start: {}", exception.getMessage());
            System.err.println("Application could not start. Check the database setup and configuration.");
        }
    }

    private static void run() {
        ConnectionFactory connectionFactory = new ConnectionFactory(DatabaseConfig.load());
        TransactionManager transactionManager = new TransactionManager(connectionFactory);
        AccountRepository accountRepository = new AccountRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        LedgerRepository ledgerRepository = new LedgerRepository();
        AuditRepository auditRepository = new AuditRepository();
        AccountService accountService = new AccountService(transactionManager, accountRepository, auditRepository);
        TransactionService transactionService = new TransactionService(transactionManager, accountRepository,
                transactionRepository, ledgerRepository, auditRepository, accountService);
        StatementService statementService = new StatementService(transactionManager, accountRepository,
                transactionRepository, ledgerRepository);

        try (Scanner scanner = new Scanner(System.in)) {
            AccountController accountController = new AccountController(scanner, accountService, transactionService);
            TransactionController transactionController = new TransactionController(scanner, accountController,
                    accountService, transactionService, statementService);
            runMenu(scanner, accountController, transactionController);
        }
    }

    private static void runMenu(Scanner scanner, AccountController accounts, TransactionController transactions) {
        boolean running = true;
        while (running) {
            System.out.println("""
                    =============================================
                           CORE BANKING LEDGER SYSTEM
                    =============================================
                    1. Create Account
                    2. View Account Details
                    3. Deposit Money
                    4. Withdraw Money
                    5. Transfer Money
                    6. View Transaction History
                    7. Generate Account Statement
                    8. Freeze Account
                    9. Reactivate Account
                    10. Exit
                    """);
            System.out.print("Enter your choice: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            try {
                int choice = InputValidator.requireMenuChoice(scanner.nextLine().trim(), 1, 10);
                switch (choice) {
                    case 1 -> accounts.createAccount();
                    case 2 -> accounts.viewAccount();
                    case 3 -> accounts.deposit();
                    case 4 -> accounts.withdraw();
                    case 5 -> transactions.transfer();
                    case 6 -> transactions.viewHistory();
                    case 7 -> transactions.generateStatement();
                    case 8 -> transactions.freezeAccount();
                    case 9 -> transactions.reactivateAccount();
                    case 10 -> running = false;
                    default -> throw new IllegalStateException("Validated menu choice was out of range.");
                }
            } catch (RuntimeException exception) {
                System.out.println("Request could not be completed: " + exception.getMessage());
            }
            System.out.println();
        }
    }
}
