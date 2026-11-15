package com.bank.service;

import com.bank.util.InputValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputValidatorTest {
    @Test
    void acceptsValidAccountNumberAndNormalizesMoneyScale() {
        assertEquals("1234567890", InputValidator.requireAccountNumber("1234567890"));
        assertEquals(new BigDecimal("12.30"),
                InputValidator.requirePositiveAmount(new BigDecimal("12.3")));
    }

    @Test
    void rejectsInvalidAccountNumbersAndAmounts() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.requireAccountNumber("12-345"));
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.requirePositiveAmount(new BigDecimal("0")));
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.requirePositiveAmount(new BigDecimal("1.001")));
    }

    @Test
    void validatesMenuRangeAndRequiredText() {
        assertEquals(2, InputValidator.requireMenuChoice("2", 1, 10));
        assertEquals("A name", InputValidator.requireText(" A name ", "Name", 30));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.requireMenuChoice("11", 1, 10));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.requireText(" ", "Name", 30));
    }
}
