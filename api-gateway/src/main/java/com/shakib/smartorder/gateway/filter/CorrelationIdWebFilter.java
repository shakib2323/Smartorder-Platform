package com.shakib.smartorder.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/*
  Gives every request a correlation id: reuses a safe incoming X-Correlation-Id or creates one,
  forwards it to the services, and returns it to the client. One id then ties together the
  gateway log line and the logs of every service that handled the request.
  It is a WebFilter (not a route filter), so even unrouted requests such as 404s get an id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String ATTRIBUTE = "smartorder.correlationId";

    // Only safe characters are accepted, so a client cannot inject fake log lines.
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain)
    {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (incoming != null && SAFE.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        exchange.getAttributes().put(ATTRIBUTE, id);

        // Set at commit time with set(), not add(): the service also echoes the header,
        // and this keeps exactly one value in the response.
        exchange.getResponse().beforeCommit(() -> 
        {
            exchange.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });

        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER, id).build();
        return chain.filter(exchange.mutate().request(request).build());
    }
}
