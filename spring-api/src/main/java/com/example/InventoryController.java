package com.example;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryRepository repository;
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryController(InventoryRepository repository,
                               OutboxRepository outboxRepository,
                               KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    private String ensureCorrelationId(String corrId) {
        if (corrId == null || corrId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return corrId;
    }

    @PostMapping("/inventory")
    public ResponseEntity<Inventory> addInventory(@RequestHeader(value = "X-Correlation-ID", required = false) String corrId,
                                                  @RequestBody Inventory inventory) {
        String correlationId = ensureCorrelationId(corrId);
        MDC.put("correlationId", correlationId);
        try {
            // save to DB
            Inventory saved = repository.save(inventory);

            // create outbox event
            OutboxEvent evt = OutboxEvent.forInventoryUpdated(saved.getProductId(), saved.getQuantity(), correlationId);
            outboxRepository.save(evt);

            return ResponseEntity.ok(saved);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestHeader(value = "X-Correlation-ID", required = false) String corrId,
                                             @RequestBody Order order) {
        String correlationId = ensureCorrelationId(corrId);
        MDC.put("correlationId", correlationId);
        try {
            // load inventory
            Inventory inventory = repository.findById(order.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            if (inventory.getQuantity() < order.getQuantity()) {
                throw new RuntimeException("Out of stock");
            }
            inventory.setQuantity(inventory.getQuantity() - order.getQuantity());
            repository.save(inventory);

            // save order -- in this sample we simply return the order object
            // In production you should persist orders in a repository as well
            // create outbox events for order.created and inventory.updated
            OutboxEvent orderEvt = OutboxEvent.forOrderCreated(order.getOrderId(), order.getProductId(), order.getQuantity(), correlationId);
            OutboxEvent invEvt = OutboxEvent.forInventoryUpdated(inventory.getProductId(), inventory.getQuantity(), correlationId);
            outboxRepository.save(orderEvt);
            outboxRepository.save(invEvt);

            return ResponseEntity.ok(order);
        } finally {
            MDC.remove("correlationId");
        }
    }
}