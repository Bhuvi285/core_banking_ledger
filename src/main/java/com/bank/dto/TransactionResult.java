package com.bank.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionResult {
    private final String transactionReference;
    private final String transactionStatus;
    private final BigDecimal transactionAmount;
    private final String message;
    private final OffsetDateTime timestamp;

    public TransactionResult(String transactionReference, String transactionStatus, BigDecimal transactionAmount,
                             String message, OffsetDateTime timestamp) {
        this.transactionReference = transactionReference;
        this.transactionStatus = transactionStatus;
        this.transactionAmount = transactionAmount;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getTransactionReference() { return transactionReference; }
    public String getTransactionStatus() { return transactionStatus; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public String getMessage() { return message; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
