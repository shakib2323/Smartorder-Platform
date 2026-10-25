package com.shakib.smartorder.order.billing;

import java.math.BigDecimal;
import java.util.List;

// One order line before pricing. listPrice is per unit and already includes GST.
public record LineInput(Long productId, int quantity, BigDecimal listPrice, BigDecimal gstRatePercent,
        List<OfferTerms> offers) {
}
