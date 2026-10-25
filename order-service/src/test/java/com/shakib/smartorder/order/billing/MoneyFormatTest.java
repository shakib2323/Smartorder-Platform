package com.shakib.smartorder.order.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyFormatTest {

    @Test
    void groupsInrTheIndianWay() {
        assertEquals("12,34,567.50", MoneyFormat.number(new BigDecimal("1234567.5"), "INR"));
        assertEquals("999.00", MoneyFormat.number(new BigDecimal("999"), "INR"));
        assertEquals("1,000.00", MoneyFormat.number(new BigDecimal("1000"), "INR"));
        assertEquals("1,00,000.00", MoneyFormat.number(new BigDecimal("100000"), "INR"));
    }

    @Test
    void groupsOtherCurrenciesInThrees() {
        assertEquals("1,234,567.50", MoneyFormat.number(new BigDecimal("1234567.5"), "USD"));
    }

    @Test
    void addsASymbolAndKeepsTheSign() {
        assertEquals("Rs. 1,299.00", MoneyFormat.withSymbol(new BigDecimal("1299"), "INR"));
        assertEquals("USD 5.00", MoneyFormat.withSymbol(new BigDecimal("5"), "USD"));
        assertEquals("-12.35", MoneyFormat.number(new BigDecimal("-12.345"), "USD"));
    }
}
