package com.bank.controller;

import com.bank.enums.AccountType;
import com.bank.model.Account;
import com.bank.service.AccountService;
import com.bank.service.TransactionService;
import com.bank.util.InputValidator;

import java.math.BigDecimal;
import java.util.Scanner;

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
        System.out.println(accountService.getAccount(scanner.nextLine().trim()));
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
