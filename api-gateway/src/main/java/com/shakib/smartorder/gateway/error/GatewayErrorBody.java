package com.shakib.smartorder.gateway.error;

import java.time.Instant;

public record GatewayErrorBody(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId) {
}
