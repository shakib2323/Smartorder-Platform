package com.shakib.smartorder.order.config;

import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.shakib.smartorder.order.filter.CorrelationIdFilter;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productRestClient(@Value("${product-service.base-url}") String baseUrl,
            @Value("${product-service.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${product-service.read-timeout-ms:2000}") long readTimeoutMs) 
    {

        // Hard timeouts: a slow dependency must never hold our threads indefinitely.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) ->
                {
                    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().set(CorrelationIdFilter.HEADER, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
