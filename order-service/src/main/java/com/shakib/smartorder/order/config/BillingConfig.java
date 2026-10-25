package com.shakib.smartorder.order.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shakib.smartorder.order.billing.ShippingPolicy;

@Configuration
public class BillingConfig {

    //Delivery fee, waived for orders at or above the free-shipping limit. Set the fee to 0 to always ship free.
    @Bean
    public ShippingPolicy shippingPolicy(@Value("${billing.shipping.flat-fee:49.00}") BigDecimal flatFee,
            @Value("${billing.shipping.free-above:999.00}") BigDecimal freeAbove) {
        return new ShippingPolicy(flatFee, freeAbove);
    }
}
