package com.shakib.smartorder.gateway.filter;

import java.util.List;
import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/*
  Rejects paths with dot segments, encoded slashes and similar tricks.
  Why: the gateway matches "/api/v1/orders/**" on the raw path and forwards it unchanged.
  A path such as /api/v1/orders/../../internal/... could match the route, and the service's
  web server would then normalize it into a path that the gateway never meant to expose.
  Failing closed here keeps /internal/** unreachable from outside.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class PathGuardWebFilter implements WebFilter {

    private static final List<String> FORBIDDEN = List.of(
            "..", "//", "\\", ";", "%2e", "%2f", "%5c", "%3b", "%00");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain)
    {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        if (rawPath != null) {
            String lower = rawPath.toLowerCase(Locale.ROOT);
            for (String forbidden : FORBIDDEN) {
                if (lower.contains(forbidden)) {
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Invalid characters in request path"));
                }
            }
        }
        return chain.filter(exchange);
    }
}
