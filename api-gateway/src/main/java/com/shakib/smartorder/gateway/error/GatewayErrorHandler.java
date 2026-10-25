package com.shakib.smartorder.gateway.error;

import java.net.ConnectException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.gateway.filter.CorrelationIdWebFilter;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler
{

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);
    private static final int MAX_CAUSE_DEPTH = 10;

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) 
    {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted())
        {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String path = exchange.getRequest().getPath().value();
        String correlationId = exchange.getAttribute(CorrelationIdWebFilter.ATTRIBUTE);

        if (status.is5xxServerError()) 
        {
            log.error("{} {} failed with {} [correlationId={}]: {}",
                    exchange.getRequest().getMethod().name(),
                    path, status.value(), correlationId,
                    ex.toString());
        } else
        {
            log.warn("{} {} rejected with {} [correlationId={}]",
                    exchange.getRequest().getMethod().name(), path, status.value(), correlationId);
        }

        GatewayErrorBody body = new GatewayErrorBody(Instant.now(), status.value(), status.getReasonPhrase(),
                message(status, ex), path, correlationId);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e)
        {
            return Mono.error(ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private HttpStatus resolveStatus(Throwable ex)
    {
        Throwable t = ex;
        for (int depth = 0; t != null && depth < MAX_CAUSE_DEPTH; depth++, t = t.getCause()) {
            if (t instanceof ConnectException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
        }
        if (ex instanceof ResponseStatusException rse) 
        {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            if (resolved != null) {
                return resolved;
            }
        }
        t = ex;
        for (int depth = 0; t != null && depth < MAX_CAUSE_DEPTH; depth++, t = t.getCause()) 
        {
            if (t instanceof TimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String message(HttpStatus status, Throwable ex) {
        if (status == HttpStatus.BAD_REQUEST && ex instanceof ResponseStatusException rse
                && rse.getReason() != null) {
            return rse.getReason();
        }
        return switch (status)
        {
            case NOT_FOUND -> "No route found for this path";
            case SERVICE_UNAVAILABLE -> "Service temporarily unavailable, please retry shortly";
            case GATEWAY_TIMEOUT -> "The service took too long to respond";
            default -> status.is4xxClientError() ? status.getReasonPhrase() : "Internal server error";
        };
    }
}
