package com.bank.model;

import com.bank.enums.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Transaction {
    private Long id;
    private String transactionReference;
    private TransactionType transactionType;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String transactionStatus;
    private String description;
    private OffsetDateTime createdAt;

    public Transaction(Long id, String transactionReference, TransactionType transactionType, Long sourceAccountId,
                       Long destinationAccountId, BigDecimal amount, String transactionStatus, String description,
                       OffsetDateTime createdAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.transactionType = transactionType;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.transactionStatus = transactionStatus;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public Long getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(Long sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public Long getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(Long destinationAccountId) { this.destinationAccountId = destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "%s | %s | amount=%s | status=%s | %s"
                .formatted(transactionReference, transactionType, amount, transactionStatus, createdAt);
    }
}
