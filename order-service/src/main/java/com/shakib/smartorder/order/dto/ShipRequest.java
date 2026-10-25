package com.shakib.smartorder.order.dto;

import jakarta.validation.constraints.Size;

public record ShipRequest(
        @Size(max = 60, message = "trackingNumber must be at most 60 characters")
        String trackingNumber) {
}
