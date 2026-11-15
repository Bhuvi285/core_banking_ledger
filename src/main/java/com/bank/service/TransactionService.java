package com.bank.service;

import com.bank.dto.TransactionResult;
import com.bank.dto.TransferRequest;
import com.bank.enums.TransactionType;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankException;
import com.bank.exception.InsufficientFundsException;
import com.bank.infrastructure.TransactionManager;
import com.bank.model.Account;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditRepository;
import com.bank.repository.LedgerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.util.IdGenerator;
import com.bank.util.InputValidator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionService {
    private static final String SUCCESS = "SUCCESS";
    private final TransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AuditRepository auditRepository;
    private final AccountService accountService;

    public TransactionService(TransactionManager transactionManager, AccountRepository accountRepository,
                              TransactionRepository transactionRepository, LedgerRepository ledgerRepository,
                              AuditRepository auditRepository, AccountService accountService) {
        this.transactionManager = transactionManager;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.auditRepository = auditRepository;
        this.accountService = accountService;
    }

    public TransactionResult deposit(String accountNumber, BigDecimal amount, String description) {
        String validNumber = InputValidator.requireAccountNumber(accountNumber);
        BigDecimal validAmount = InputValidator.requirePositiveAmount(amount);
        String validDescription = normalizeDescription(description);
        return transactionManager.execute(connection -> {
            Account account = accountRepository.findByAccountNumberForUpdate(connection, validNumber)
                    .orElseThrow(() -> new AccountNotFoundException(validNumber));
            accountService.validateActive(account);
            Transaction transaction = createTransaction(TransactionType.DEPOSIT, null, account.getId(),
                    validAmount, validDescription);
            Transaction saved = transactionRepository.save(connection, transaction);
            accountRepository.updateBalance(connection, account.getId(), account.getBalance().add(validAmount));
            ledgerRepository.saveCredit(connection, saved.getId(), account.getId(), validAmount);
            auditRepository.save(connection, "DEPOSIT_COMPLETED", "TRANSACTION", saved.getId(),
                    "Deposit completed.");
            return result(saved, "Deposit completed.");
        });
    }

    public TransactionResult withdraw(String accountNumber, BigDecimal amount, String description) {
        String validNumber = InputValidator.requireAccountNumber(accountNumber);
        BigDecimal validAmount = InputValidator.requirePositiveAmount(amount);
        String validDescription = normalizeDescription(description);
        return transactionManager.execute(connection -> {
            Account account = accountRepository.findByAccountNumberForUpdate(connection, validNumber)
                    .orElseThrow(() -> new AccountNotFoundException(validNumber));
            accountService.validateActive(account);
            if (account.getBalance().compareTo(validAmount) < 0) {
                throw new InsufficientFundsException("The account has insufficient funds for this withdrawal.");
            }
            Transaction transaction = createTransaction(TransactionType.WITHDRAWAL, account.getId(), null,
                    validAmount, validDescription);
            Transaction saved = transactionRepository.save(connection, transaction);
            accountRepository.updateBalance(connection, account.getId(), account.getBalance().subtract(validAmount),
                    validAmount);
            ledgerRepository.saveDebit(connection, saved.getId(), account.getId(), validAmount);
            auditRepository.save(connection, "WITHDRAWAL_COMPLETED", "TRANSACTION", saved.getId(),
                    "Withdrawal completed.");
            return result(saved, "Withdrawal completed.");
        });
    }

    public TransactionResult transfer(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request is required.");
        }
        String sourceNumber = InputValidator.requireAccountNumber(request.getSourceAccountNumber());
        String destinationNumber = InputValidator.requireAccountNumber(request.getDestinationAccountNumber());
        if (sourceNumber.equals(destinationNumber)) {
            throw new BankException("Source and destination accounts must be different.");
        }
        BigDecimal amount = InputValidator.requirePositiveAmount(request.getAmount());
        String description = normalizeDescription(request.getDescription());

        return transactionManager.execute(connection -> {
            List<Account> lockedAccounts = accountRepository.findByAccountNumbersForUpdate(
                    connection, sourceNumber, destinationNumber);
            Map<String, Account> accounts = new HashMap<>();
            for (Account account : lockedAccounts) {
                accounts.put(account.getAccountNumber(), account);
            }
            Account source = accounts.get(sourceNumber);
            Account destination = accounts.get(destinationNumber);
            if (source == null) {
                throw new AccountNotFoundException(sourceNumber);
            }
            if (destination == null) {
                throw new AccountNotFoundException(destinationNumber);
            }
            accountService.validateActive(source);
            accountService.validateActive(destination);
            if (source.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("The source account has insufficient funds for this transfer.");
            }

            Transaction transaction = createTransaction(TransactionType.TRANSFER, source.getId(), destination.getId(),
                    amount, description);
            Transaction saved = transactionRepository.save(connection, transaction);
            accountRepository.updateBalance(connection, source.getId(), source.getBalance().subtract(amount), amount);
            accountRepository.updateBalance(connection, destination.getId(), destination.getBalance().add(amount));
            ledgerRepository.saveDebit(connection, saved.getId(), source.getId(), amount);
            ledgerRepository.saveCredit(connection, saved.getId(), destination.getId(), amount);
            auditRepository.save(connection, "TRANSFER_COMPLETED", "TRANSACTION", saved.getId(),
                    "Transfer completed.");
            return result(saved, "Transfer completed.");
        });
    }

    private static Transaction createTransaction(TransactionType type, Long sourceId, Long destinationId,
                                                 BigDecimal amount, String description) {
        return new Transaction(null, IdGenerator.transactionReference(), type, sourceId, destinationId, amount,
                SUCCESS, description, null);
    }

    private static TransactionResult result(Transaction transaction, String message) {
        return new TransactionResult(transaction.getTransactionReference(), transaction.getTransactionStatus(),
                transaction.getAmount(), message, transaction.getCreatedAt());
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return InputValidator.requireText(description, "Description", 255);
    }
}
