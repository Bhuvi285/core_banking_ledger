package com.bank.controller;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.Account;
import com.bank.service.AccountService;
import com.bank.service.TransactionService;
import com.bank.util.InputValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AccountController {
    private final Scanner scanner;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(Scanner scanner, AccountService accountService, TransactionService transactionService) {
        this.scanner = scanner;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    public void createAccount() {
        System.out.print("Account holder name: ");
        String name = scanner.nextLine();
        System.out.print("Account type (SAVINGS/CURRENT): ");
        AccountType type = AccountType.valueOf(scanner.nextLine().trim().toUpperCase());
        Account account = accountService.createAccount(name, type);
        System.out.println("Account created: " + account.getAccountNumber());
    }

    public void viewAccount() {
        System.out.print("Account number: ");
        try {
            displayAccountDetails(accountService.getAccount(scanner.nextLine().trim()));
        } catch (AccountNotFoundException exception) {
            System.out.println("Account not found. Please check the account number and try again.");
        }
    }

    public void viewAllAccounts() {
        displayAccountDirectory(false);
    }

    public List<Account> displayAccountDirectory(boolean activeOnly) {
        List<Account> accounts = accountService.getAllAccounts();
        List<Account> displayedAccounts = activeOnly
                ? accounts.stream().filter(account -> account.getAccountStatus() == AccountStatus.ACTIVE)
                        .collect(Collectors.toList())
                : accounts;
        if (activeOnly) {
            System.out.println("--------------------------------------------------------------");
        } else {
            System.out.println("""
                    ==============================================
                                   ACCOUNT DIRECTORY
                    ==============================================""");
        }
        if (displayedAccounts.isEmpty()) {
            System.out.println(activeOnly
                    ? "No active accounts are currently available."
                    : "No accounts are currently available.");
            System.out.println("==============================================");
            return displayedAccounts;
        }
        printAccountDirectoryRows(displayedAccounts);
        System.out.println("--------------------------------------------------------------");
        System.out.printf(Locale.ROOT, "Total Accounts: %d%n", displayedAccounts.size());
        System.out.println("==============================================");
        return displayedAccounts;
    }

    public void displayAccountDetails(Account account) {
        if (account == null) {
            System.out.println("Account details are not available.");
            return;
        }
        System.out.println("""
                ==============================================
                               ACCOUNT DETAILS
                ==============================================""");
        System.out.printf(Locale.ROOT, "%-20s : %s%n", "Account ID", account.getId());
        System.out.printf(Locale.ROOT, "%-20s : %s%n", "Account Number", account.getAccountNumber());
        System.out.printf(Locale.ROOT, "%-20s : %s%n", "Account Holder", account.getAccountHolderName());
        System.out.printf(Locale.ROOT, "%-20s : %s%n", "Account Type", account.getAccountType());
        System.out.printf(Locale.ROOT, "%-20s : Rs. %s%n", "Available Balance",
                account.getBalance() == null ? "N/A" : account.getBalance().setScale(2).toPlainString());
        System.out.printf(Locale.ROOT, "%-20s : %s%n", "Account Status", account.getAccountStatus());
        System.out.println("==============================================");
    }

    private static void printAccountDirectoryRows(List<Account> accounts) {
        System.out.printf(Locale.ROOT, "%-16s %-25s %-10s %-10s%n",
                "Account No.", "Account Holder", "Type", "Status");
        System.out.println("--------------------------------------------------------------");
        for (Account account : accounts) {
            System.out.printf(Locale.ROOT, "%-16s %-25s %-10s %-10s%n",
                    account.getAccountNumber(), fitHolderName(account.getAccountHolderName()),
                    account.getAccountType(), account.getAccountStatus());
        }
    }

    private static String fitHolderName(String name) {
        if (name == null || name.length() <= 25) {
            return name;
        }
        return name.substring(0, 22) + "...";
    }

    public void deposit() {
        String accountNumber = promptAccountNumber();
        BigDecimal amount = promptAmount();
        System.out.print("Description (optional): ");
        System.out.println(transactionService.deposit(accountNumber, amount, scanner.nextLine()).getMessage());
    }

    public void withdraw() {
        String accountNumber = promptAccountNumber();
        BigDecimal amount = promptAmount();
        System.out.print("Description (optional): ");
        System.out.println(transactionService.withdraw(accountNumber, amount, scanner.nextLine()).getMessage());
    }

    public String promptAccountNumber() {
        System.out.print("Account number: ");
        return InputValidator.requireAccountNumber(scanner.nextLine().trim());
    }

    public BigDecimal promptAmount() {
        System.out.print("Amount: ");
        return InputValidator.requirePositiveAmount(new BigDecimal(scanner.nextLine().trim()));
    }

}
