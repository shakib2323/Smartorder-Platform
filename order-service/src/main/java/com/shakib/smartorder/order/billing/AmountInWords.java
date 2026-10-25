package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Rupees One Thousand Two Hundred Thirty Four and Fifty Paise Only, Indian system (lakh, crore). INR only.
public final class AmountInWords {

    private static final String[] ONES = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
            "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen" };
    private static final String[] TENS = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy",
            "Eighty", "Ninety" };

    private AmountInWords() {
    }

    // @return the words, or null for a currency this class does not spell out
    public static String rupees(BigDecimal amount, String currency) 
    {
        if (!"INR".equals(currency)) 
        {
            return null;
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP).abs();
        long rupees = rounded.toBigInteger().longValueExact();
        int paise = rounded.remainder(BigDecimal.ONE).movePointRight(2).intValueExact();

        StringBuilder out = new StringBuilder("Rupees ");
        out.append(rupees == 0 ? "Zero" : words(rupees));
        if (paise > 0) {
            out.append(" and ").append(words(paise)).append(" Paise");
        }
        return out.append(" Only").toString();
    }

    private static String words(long n) 
    {
        StringBuilder out = new StringBuilder();
        long crore = n / 10_000_000L;
        n %= 10_000_000L;
        long lakh = n / 100_000L;
        n %= 100_000L;
        long thousand = n / 1_000L;
        n %= 1_000L;

        if (crore > 0) 
        {
            out.append(words(crore)).append(" Crore ");
        }
        if (lakh > 0) 
        {
            out.append(below100(lakh)).append(" Lakh ");
        }
        if (thousand > 0) 
        {
            out.append(below100(thousand)).append(" Thousand ");
        }
        if (n >= 100) 
        {
            out.append(ONES[(int) (n / 100)]).append(" Hundred ");
            n %= 100;
            if (n > 0) 
            {
                out.append("and ");
            }
        }
        if (n > 0) 
        {
            out.append(below100(n));
        }
        return out.toString().trim();
    }

    private static String below100(long n) 
    {
        if (n < 20) 
        {
            return ONES[(int) n];
        }
        return TENS[(int) (n / 10)] + (n % 10 == 0 ? "" : " " + ONES[(int) (n % 10)]);
    }
}
