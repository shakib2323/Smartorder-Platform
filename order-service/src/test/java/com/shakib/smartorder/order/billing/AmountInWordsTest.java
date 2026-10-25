package com.shakib.smartorder.order.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AmountInWordsTest {

    @Test
    void spellsRupeesAndPaise() {
        assertEquals("Rupees One Thousand Two Hundred and Thirty Four and Fifty Paise Only",
                AmountInWords.rupees(new BigDecimal("1234.50"), "INR"));
    }

    @Test
    void usesTheIndianSystem() {
        assertEquals("Rupees One Lakh Only", AmountInWords.rupees(new BigDecimal("100000"), "INR"));
        assertEquals("Rupees Twelve Lakh Thirty Four Thousand Five Hundred and Sixty Seven Only",
                AmountInWords.rupees(new BigDecimal("1234567"), "INR"));
        assertEquals("Rupees One Crore Only", AmountInWords.rupees(new BigDecimal("10000000"), "INR"));
    }

    @Test
    void handlesZeroAndSmallNumbers() {
        assertEquals("Rupees Zero Only", AmountInWords.rupees(BigDecimal.ZERO, "INR"));
        assertEquals("Rupees Nineteen Only", AmountInWords.rupees(new BigDecimal("19.00"), "INR"));
        assertEquals("Rupees Twenty One Only", AmountInWords.rupees(new BigDecimal("21"), "INR"));
        assertEquals("Rupees One and One Paise Only", AmountInWords.rupees(new BigDecimal("1.01"), "INR"));
    }

    @Test
    void otherCurrenciesAreNotSpelledOut() {
        assertNull(AmountInWords.rupees(new BigDecimal("10"), "USD"));
    }
}
