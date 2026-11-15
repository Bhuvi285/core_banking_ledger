package com.bank.util;

import java.math.BigDecimal;

public final class InputValidator {
    private static final int MAX_AMOUNT_PRECISION = 19;
    private static final int MAX_AMOUNT_SCALE = 2;

    private InputValidator() {
    }

    public static String requireAccountNumber(String accountNumber) {
        if (accountNumber == null || !accountNumber.matches("\\d{6,32}")) {
            throw new IllegalArgumentException("Account number must contain 6 to 32 digits.");
        }
        return accountNumber;
    }

    public static BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > MAX_AMOUNT_SCALE
                || amount.precision() > MAX_AMOUNT_PRECISION) {
            throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,2).");
        }
        return amount.setScale(MAX_AMOUNT_SCALE);
    }

    public static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is required and must be at most "
                    + maxLength + " characters.");
        }
        return value.trim();
    }

    public static int requireMenuChoice(String input, int minimum, int maximum) {
        try {
            int choice = Integer.parseInt(input);
            if (choice >= minimum && choice <= maximum) {
                return choice;
            }
        } catch (NumberFormatException ignored) {
            // Return the same user-facing validation error for malformed and out-of-range choices.
        }
        throw new IllegalArgumentException("Choose a menu option from " + minimum + " to " + maximum + ".");
    }
}
