package com.shakib.smartorder.inventory.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakib.smartorder.inventory.event.ProductCreatedEvent;
import com.shakib.smartorder.inventory.service.InventoryService;

/*
  Creates the stock row when a product is created. The processed mark and the new row commit
  in one transaction, so a message delivered twice creates the stock once.
 */
@Component
public class ProductEventHandler {

    public static final String PRODUCT_CREATED = "ProductCreated";

    private static final Logger log = LoggerFactory.getLogger(ProductEventHandler.class);

    private final InventoryService inventoryService;
    private final ProcessedEventStore processedEvents;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;

    public ProductEventHandler(InventoryService inventoryService, ProcessedEventStore processedEvents,
            TransactionTemplate tx, ObjectMapper objectMapper) 
    {
        this.inventoryService = inventoryService;
        this.processedEvents = processedEvents;
        this.tx = tx;
        this.objectMapper = objectMapper;
    }

    public void handle(String eventId, String eventType, String payload) 
    {
        if (!PRODUCT_CREATED.equals(eventType))
        {
            log.warn("Ignoring unknown event type {} (eventId={})", eventType, eventId);
            return;
        }
        ProductCreatedEvent event = read(payload);
        tx.executeWithoutResult(status -> {
            if (!processedEvents.markProcessed(eventId))
            {
                log.info("Event {} already processed, skipping", eventId);
                return;
            }
            inventoryService.createForProduct(event.productId(), event.sellerId(), event.initialStock());
            log.info("Stock created for product {} (seller {}, quantity {})", event.productId(), event.sellerId(),
                    event.initialStock());
        });
    }

    private ProductCreatedEvent read(String payload)
    {
        try {
            return objectMapper.readValue(payload, ProductCreatedEvent.class);
        } catch (JsonProcessingException e) {
            // Not retryable: the same bad message will never become valid. It goes to the dead-letter topic.
            throw new IllegalArgumentException("Malformed ProductCreatedEvent payload", e);
        }
    }
}
