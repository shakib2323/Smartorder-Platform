package com.shakib.smartorder.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Where the order should be delivered. Everything except line2 is required. */
public record ShippingAddressRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must be at most 100 characters")
        String fullName,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$",
                message = "phone must be 7 to 20 characters: digits, spaces, brackets, dashes, optional leading +")
        String phone,

        @NotBlank(message = "line1 is required")
        @Size(max = 200, message = "line1 must be at most 200 characters")
        String line1,

        // optional: flat, floor, landmark and so on
        @Size(max = 200, message = "line2 must be at most 200 characters")
        String line2,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @NotBlank(message = "state is required")
        @Size(max = 100, message = "state must be at most 100 characters")
        String state,

        @NotBlank(message = "postalCode is required")
        @Size(max = 20, message = "postalCode must be at most 20 characters")
        String postalCode,

        @NotBlank(message = "country is required")
        @Size(max = 100, message = "country must be at most 100 characters")
        String country) {
}
