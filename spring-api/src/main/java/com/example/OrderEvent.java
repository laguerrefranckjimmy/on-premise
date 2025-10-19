package com.example;

import java.time.Instant;
import java.util.UUID;

public class OrderEvent {

    private String eventId;
    private String eventType; // e.g., "ORDER_CREATED", "ORDER_UPDATED", "ORDER_CANCELLED"
    private Order order;
    private Instant eventTime;
    private String correlationId; // for tracing requests end-to-end

    public OrderEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventTime = Instant.now();
    }

    public OrderEvent(String eventType, Order order, String correlationId) {
        this();
        this.eventType = eventType;
        this.order = order;
        this.correlationId = correlationId;
    }

    public OrderEvent(String orderId, String productId, int quantity) {
    }

    // ======= Getters & Setters =======

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // ======= Utility Methods =======

    @Override
    public String toString() {
        return "OrderEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", order=" + order +
                ", eventTime=" + eventTime +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
