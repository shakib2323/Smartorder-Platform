package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Formats amounts for the invoice. INR uses Indian digit grouping (12,34,567.00).
public final class MoneyFormat {

    private MoneyFormat() {
    }

    //Rs. 1,299.00 for INR, USD 1,299.00 for other currencies. Plain ASCII so any PDF font can print it.
    public static String withSymbol(BigDecimal amount, String currency) 
    {
        String prefix = "INR".equals(currency) ? "Rs. " : currency + " ";
        return prefix + number(amount, currency);
    }

    public static String number(BigDecimal amount, String currency) 
    {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        String sign = rounded.signum() < 0 ? "-" : "";
        String[] parts = rounded.abs().toPlainString().split("\\.");
        return sign + ("INR".equals(currency) ? groupIndian(parts[0]) : groupWestern(parts[0])) + "." + parts[1];
    }

    private static String groupWestern(String digits)
    {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) 
        {
            if (count > 0 && count % 3 == 0) {
                out.append(',');
            }
            out.append(digits.charAt(i));
            count++;
        }
        return out.reverse().toString();
    }

    private static String groupIndian(String digits) 
    {
        if (digits.length() <= 3) 
        {
            return digits;
        }
        String lastThree = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = rest.length() - 1; i >= 0; i--) 
        {
            if (count > 0 && count % 2 == 0) {
                out.append(',');
            }
            out.append(rest.charAt(i));
            count++;
        }
        return out.reverse() + "," + lastThree;
    }
}
