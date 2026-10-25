package com.shakib.smartorder.inventory.dto;


import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReserveRequest(
        @NotNull(message = "orderId is required")
        Long orderId,

        @NotEmpty(message = "items must not be empty")
        List<@Valid ReserveLine> items) {
}

