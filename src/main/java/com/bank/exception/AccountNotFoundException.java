package com.bank.exception;

public class AccountNotFoundException extends BankException {
    public AccountNotFoundException(String accountNumber) {
        super("No account found for account number " + accountNumber + ".");
    }
}
