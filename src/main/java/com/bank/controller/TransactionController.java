package com.bank.controller;

import com.bank.dto.TransferRequest;
import com.bank.enums.AccountStatus;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.service.AccountService;
import com.bank.service.StatementService;
import com.bank.service.TransactionService;
import com.bank.util.InputValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Scanner;

public class TransactionController {
    private final Scanner scanner;
    private final AccountController accountController;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final StatementService statementService;

    public TransactionController(Scanner scanner, AccountController accountController, AccountService accountService,
                                 TransactionService transactionService, StatementService statementService) {
        this.scanner = scanner;
        this.accountController = accountController;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.statementService = statementService;
    }

    public void transfer() {
        System.out.println("""
                ==============================================
                                MONEY TRANSFER
                ==============================================""");
        System.out.println("AVAILABLE ACCOUNTS");
        List<Account> eligibleAccounts = accountController.displayAccountDirectory(true);
        if (eligibleAccounts.size() < 2) {
            System.out.println("At least two ACTIVE accounts are required to make a transfer.");
            return;
        }

        System.out.print("Source account number: ");
        String source = scanner.nextLine().trim();
        if (!isAvailableAccount(source, eligibleAccounts)) {
            System.out.println("Source account is not an available ACTIVE account.");
            return;
        }
        System.out.print("Destination account number: ");
        String destination = scanner.nextLine().trim();
        if (!isAvailableAccount(destination, eligibleAccounts)) {
            System.out.println("Destination account is not an available ACTIVE account.");
            return;
        }
        if (source.equals(destination)) {
            System.out.println("Source and destination accounts must be different.");
            return;
        }
        System.out.print("Amount: ");
        BigDecimal amount = InputValidator.requirePositiveAmount(new BigDecimal(scanner.nextLine().trim()));
        System.out.print("Description (optional): ");
        String description = scanner.nextLine();
        System.out.print("Confirm Transfer? (Y/N): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Transfer cancelled.");
            return;
        }
        System.out.println("SUCCESS: " + transactionService.transfer(
                new TransferRequest(source, destination, amount, description)).getMessage());
    }

    private static boolean isAvailableAccount(String accountNumber, List<Account> accounts) {
        try {
            String validNumber = InputValidator.requireAccountNumber(accountNumber);
            return accounts.stream().anyMatch(account -> account.getAccountStatus() == AccountStatus.ACTIVE
                    && account.getAccountNumber().equals(validNumber));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public void viewHistory() {
        String accountNumber = accountController.promptAccountNumber();
        List<Transaction> transactions = statementService.getTransactionHistory(
                accountNumber, OffsetDateTime.parse("1970-01-01T00:00:00Z"), OffsetDateTime.now());
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        transactions.forEach(System.out::println);
    }

    public void generateStatement() {
        String accountNumber = accountController.promptAccountNumber();
        System.out.print("Start date (YYYY-MM-DD): ");
        OffsetDateTime from = java.time.LocalDate.parse(scanner.nextLine().trim()).atStartOfDay().atOffset(
                java.time.ZoneOffset.UTC);
        System.out.print("End date (YYYY-MM-DD): ");
        OffsetDateTime to = java.time.LocalDate.parse(scanner.nextLine().trim()).plusDays(1).atStartOfDay()
                .minusNanos(1).atOffset(java.time.ZoneOffset.UTC);
        System.out.println(statementService.generateStatement(accountNumber, from, to));
    }

    public void freezeAccount() {
        accountService.freezeAccount(accountController.promptAccountNumber());
        System.out.println("Account frozen.");
    }

    public void reactivateAccount() {
        accountService.reactivateAccount(accountController.promptAccountNumber());
        System.out.println("Account reactivated.");
    }
}
