package com.example;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document
public class OutboxEvent {

    @Id
    private String id;

    private String topic;
    private String payload;
    private boolean sent;
    private Instant createdAt;
    private String correlationId;

    public OutboxEvent() {}

    public OutboxEvent(String topic, String payload, String correlationId) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.payload = payload;
        this.correlationId = correlationId;
        this.sent = false;
        this.createdAt = Instant.now();
    }

    public static OutboxEvent forInventoryUpdated(String productId, int quantity, String correlationId) {
        String p = String.format(
                "{\"type\":\"inventory.updated\",\"productId\":\"%s\",\"quantity\":%d,\"timestamp\":\"%s\"}",
                productId, quantity, Instant.now().toString()
        );
        return new OutboxEvent("inventory.updated", p, correlationId);
    }

    public static OutboxEvent forOrderCreated(String orderId, String productId, int quantity, String correlationId) {
        String p = String.format(
                "{\"type\":\"order.created\",\"orderId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d,\"timestamp\":\"%s\"}",
                orderId, productId, quantity, Instant.now().toString()
        );
        return new OutboxEvent("order.created", p, correlationId);
    }

    // getters and setters omitted for brevity - include as needed

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
    public String getCorrelationId() { return correlationId; }
}