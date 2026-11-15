package com.bank.dto;

import java.math.BigDecimal;

public class TransferRequest {
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final BigDecimal amount;
    private final String description;

    public TransferRequest(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount,
                           String description) {
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.description = description;
    }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
