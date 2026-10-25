package com.shakib.smartorder.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @NotBlank(message = "legalName is required")
        @Size(max = 200, message = "legalName must be at most 200 characters")
        String legalName,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @NotBlank(message = "contactEmail is required")
        @Email(message = "contactEmail is not valid")
        @Size(max = 254, message = "contactEmail must be at most 254 characters")
        String contactEmail,

        // Optional: small sellers below the GST threshold have none.
        @Pattern(regexp = "^$|^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z]Z[0-9A-Za-z]$",
                message = "gstin must be a valid 15-character GSTIN")
        String gstin,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must be at most 100 characters")
        String city,

        @NotBlank(message = "state is required")
        @Size(max = 100, message = "state must be at most 100 characters")
        String state) {
}
