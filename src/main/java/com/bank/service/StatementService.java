package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.infrastructure.TransactionManager;
import com.bank.model.Account;
import com.bank.model.LedgerEntry;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.LedgerRepository;
import com.bank.repository.TransactionRepository;
import com.bank.util.InputValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class StatementService {
    private final TransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    public StatementService(TransactionManager transactionManager, AccountRepository accountRepository,
                            TransactionRepository transactionRepository, LedgerRepository ledgerRepository) {
        this.transactionManager = transactionManager;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public List<Transaction> getTransactionHistory(String accountNumber, OffsetDateTime from, OffsetDateTime to) {
        validateDateRange(from, to);
        String validNumber = InputValidator.requireAccountNumber(accountNumber);
        return transactionManager.execute(connection -> {
            Account account = accountRepository.findByAccountNumber(connection, validNumber)
                    .orElseThrow(() -> new AccountNotFoundException(validNumber));
            return transactionRepository.findHistory(connection, account.getId(), from, to);
        });
    }

    public String generateStatement(String accountNumber, OffsetDateTime from, OffsetDateTime to) {
        validateDateRange(from, to);
        String validNumber = InputValidator.requireAccountNumber(accountNumber);
        return transactionManager.execute(connection -> {
            Account account = accountRepository.findByAccountNumberForUpdate(connection, validNumber)
                    .orElseThrow(() -> new AccountNotFoundException(validNumber));
            List<LedgerEntry> entries = ledgerRepository.findByAccount(connection, account.getId(), from, to);
            BigDecimal debitsBefore = ledgerRepository.calculateTotal(connection, account.getId(), "DEBIT", from);
            BigDecimal creditsBefore = ledgerRepository.calculateTotal(connection, account.getId(), "CREDIT", from);
            BigDecimal debitsAtEnd = ledgerRepository.calculateTotal(connection, account.getId(), "DEBIT", to.plusNanos(1));
            BigDecimal creditsAtEnd = ledgerRepository.calculateTotal(connection, account.getId(), "CREDIT", to.plusNanos(1));
            OffsetDateTime asOf = OffsetDateTime.now();
            BigDecimal allDebits = ledgerRepository.calculateTotal(connection, account.getId(), "DEBIT",
                    asOf.plusNanos(1));
            BigDecimal allCredits = ledgerRepository.calculateTotal(connection, account.getId(), "CREDIT",
                    asOf.plusNanos(1));
            BigDecimal openingBaseline = account.getBalance().subtract(allCredits.subtract(allDebits));
            BigDecimal openingBalance = openingBaseline.add(creditsBefore).subtract(debitsBefore);
            BigDecimal closingBalance = openingBaseline.add(creditsAtEnd).subtract(debitsAtEnd);

            StringBuilder statement = new StringBuilder()
                    .append("Statement for ").append(account.getAccountHolderName())
                    .append(" (").append(account.getAccountNumber()).append(")\n")
                    .append("Period: ").append(from).append(" to ").append(to).append('\n')
                    .append("Opening balance: ").append(openingBalance).append('\n');
            for (LedgerEntry entry : entries) {
                statement.append(entry.getCreatedAt()).append(" | ")
                        .append(entry.getEntryType()).append(" | ")
                        .append(entry.getAmount()).append('\n');
            }
            statement.append("Closing balance: ").append(closingBalance);
            return statement.toString();
        });
    }

    private static void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Statement start and end dates are required, and start must not be after end.");
        }
    }
}
