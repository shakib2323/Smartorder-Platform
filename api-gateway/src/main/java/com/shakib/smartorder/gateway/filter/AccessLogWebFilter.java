package com.shakib.smartorder.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/*
  One log line per request: method, path, status, time, correlation id.
  The query string is deliberately not logged, because it can carry sensitive values.
  It logs when the response is about to be sent, so the final status is always known,
  including statuses produced by the error handler.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) 
    {
        long start = System.nanoTime();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        exchange.getResponse().beforeCommit(() -> 
        {
            long millis = (System.nanoTime() - start) / 1_000_000;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            Object correlationId = exchange.getAttributes().get(CorrelationIdWebFilter.ATTRIBUTE);
            log.info("{} {} -> {} in {} ms [correlationId={}]",
                    method, path, status == null ? 200 : status.value(), millis, correlationId);
            return Mono.empty();
        });
        return chain.filter(exchange);
    }
}
