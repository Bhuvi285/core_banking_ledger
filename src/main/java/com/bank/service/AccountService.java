package com.bank.service;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankException;
import com.bank.infrastructure.TransactionManager;
import com.bank.model.Account;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.util.IdGenerator;
import com.bank.util.InputValidator;

import java.math.BigDecimal;
import java.util.List;

public class AccountService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final TransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final AuditRepository auditRepository;

    public AccountService(TransactionManager transactionManager, AccountRepository accountRepository,
                          AuditRepository auditRepository) {
        this.transactionManager = transactionManager;
        this.accountRepository = accountRepository;
        this.auditRepository = auditRepository;
    }

    public Account createAccount(String accountHolderName, AccountType accountType) {
        String holder = InputValidator.requireText(accountHolderName, "Account holder name", 120);
        if (accountType == null) {
            throw new IllegalArgumentException("Account type is required.");
        }
        return transactionManager.execute(connection -> {
            Account account = new Account(IdGenerator.accountNumber(), holder, accountType, ZERO);
            Account created = accountRepository.create(connection, account);
            auditRepository.save(connection, "ACCOUNT_CREATED", "ACCOUNT", created.getId(),
                    "Account created.");
            return created;
        });
    }

    public Account getAccount(String accountNumber) {
        String validNumber = InputValidator.requireAccountNumber(accountNumber);
        return transactionManager.execute(connection -> accountRepository.findByAccountNumber(connection, validNumber)
                .orElseThrow(() -> new AccountNotFoundException(validNumber)));
    }

    public List<Account> getAllAccounts() {
        return transactionManager.execute(accountRepository::findAll);
    }

    public void freezeAccount(String accountNumber) {
        updateStatus(accountNumber, AccountStatus.FROZEN, AccountStatus.ACTIVE, "ACCOUNT_FROZEN");
    }

    public void reactivateAccount(String accountNumber) {
        updateStatus(accountNumber, AccountStatus.ACTIVE, AccountStatus.FROZEN, "ACCOUNT_REACTIVATED");
    }

    public void validateActive(Account account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BankException("Transactions are not allowed for an account that is "
                    + account.getAccountStatus().name().toLowerCase() + ".");
        }
    }

    private void updateStatus(String accountNumber, AccountStatus newStatus, AccountStatus requiredStatus,
                              String auditAction) {
        String validNumber = InputValidator.requireAccountNumber(accountNumber);
        transactionManager.execute(connection -> {
            Account account = accountRepository.findByAccountNumberForUpdate(connection, validNumber)
                    .orElseThrow(() -> new AccountNotFoundException(validNumber));
            if (account.getAccountStatus() != requiredStatus) {
                throw new BankException("Only " + requiredStatus.name().toLowerCase()
                        + " accounts can be " + newStatus.name().toLowerCase() + ".");
            }
            accountRepository.updateStatus(connection, account.getId(), newStatus);
            auditRepository.save(connection, auditAction, "ACCOUNT", account.getId(),
                    "Account status changed to " + newStatus + ".");
            return null;
        });
    }
}
