package com.bank.model;

import com.bank.enums.AccountStatus;
import com.bank.enums.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Account {
    private Long id;
    private String accountNumber;
    private String accountHolderName;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus accountStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Account(Long id, String accountNumber, String accountHolderName, AccountType accountType,
                   BigDecimal balance, AccountStatus accountStatus, OffsetDateTime createdAt,
                   OffsetDateTime updatedAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.balance = balance;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Account(String accountNumber, String accountHolderName, AccountType accountType, BigDecimal balance) {
        this(null, accountNumber, accountHolderName, accountType, balance, AccountStatus.ACTIVE, null, null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Account{id=%d, accountNumber='%s', accountHolderName='%s', accountType=%s, balance=%s, status=%s}"
                .formatted(id, accountNumber, accountHolderName, accountType, balance, accountStatus);
    }
}
