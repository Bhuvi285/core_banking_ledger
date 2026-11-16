package com.bank.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class IdGenerator implements AccountNumberGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public IdGenerator() {
    }

    public static String transactionReference() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase();
    }

    public static String accountNumber() {
        StringBuilder accountNumber = new StringBuilder("9");
        for (int index = 0; index < 11; index++) {
            accountNumber.append(RANDOM.nextInt(10));
        }
        return accountNumber.toString();
    }

    @Override
    public String generateAccountNumber() {
        return accountNumber();
    }
}
