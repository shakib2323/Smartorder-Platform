package com.shakib.smartorder.order.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.shakib.smartorder.order.dto.ProductInfo;
import com.shakib.smartorder.order.exception.ProductServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/*
  Calls the Product service. Protection layers (configured in application.properties):
  1. Timeouts on the HTTP client.
  2. Retry with a short wait for transient failures (outermost).
  3. Circuit breaker: after repeated failures it opens and fails fast, so a down Product
     service cannot exhaust our threads. The fallback turns every technical failure into a clean 503.
 
  One batch call fetches all products of a cart or order, instead of one call per product.
 */
@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private static final int CHUNK_SIZE = 50; // the Product service accepts at most 50 ids per call

    private final RestClient restClient;

    public ProductClient(RestClient productRestClient) 
    {
        this.restClient = productRestClient;
    }

    /*
      @return the visible products by id. A product that does not exist, is inactive, or belongs to
              a brand that is not approved is simply missing from the map; the caller decides what that means.
     */
    @Retry(name = "productService", fallbackMethod = "fallback")
    @CircuitBreaker(name = "productService")
    public Map<Long, ProductInfo> getProducts(Collection<Long> productIds)
    {
        Map<Long, ProductInfo> found = new LinkedHashMap<>();
        List<Long> ids = new ArrayList<>(productIds);
        for (int from = 0; from < ids.size(); from += CHUNK_SIZE) 
        {
            List<Long> chunk = ids.subList(from, Math.min(from + CHUNK_SIZE, ids.size()));
            String joined = chunk.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<ProductInfo> products = restClient.get()
                    .uri("/api/v1/products/batch?ids={ids}", joined)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ProductInfo>>() { });
            if (products != null) 
            {
                products.forEach(p -> found.put(p.id(), p));
            }
        }
        return found;
    }

    public Map<Long, ProductInfo> fallback(Collection<Long> productIds, Throwable cause) 
    {
        log.warn("Product service call failed for products {}: {}", productIds, cause.toString());
        throw new ProductServiceUnavailableException(cause);
    }
}
