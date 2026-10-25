package com.shakib.smartorder.order.messaging;

public final class Topics {

    public static final String ORDER_EVENTS = "order-events";
    public static final String INVENTORY_EVENTS = "inventory-events";
    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    private Topics() {
    }
}
