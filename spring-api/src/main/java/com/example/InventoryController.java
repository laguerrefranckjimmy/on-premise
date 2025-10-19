package com.example;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("spring/api/inventory")
public class InventoryController {

    private final InventoryRepository repository; // Couchbase repo
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    public InventoryController(InventoryRepository repo, KafkaTemplate<String, InventoryEvent> kafkaTemplate){
        this.repository = repo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/add")
    public Inventory addInventory(@RequestBody Inventory inventory) {
        Inventory existing = repository.findById(inventory.getProductId()).orElse(new Inventory(inventory.getProductId(), 0));
        existing.setQuantity(existing.getQuantity() + inventory.getQuantity());
        repository.save(existing);

        // Publish Kafka event
        kafkaTemplate.send("inventory.updated", new InventoryEvent(existing.getProductId(), existing.getQuantity()));

        return existing;
    }

    @PostMapping("/order")
    public Order createOrder(@RequestBody Order order) {
        Inventory inventory = repository.findById(order.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if(inventory.getQuantity() < order.getQuantity()){
            throw new RuntimeException("Out of stock");
        }

        inventory.setQuantity(inventory.getQuantity() - order.getQuantity());
        repository.save(inventory);

        // Publish events
        kafkaTemplate.send("order.created", new OrderEvent(order.getOrderId(), order.getProductId(), order.getQuantity()));
        kafkaTemplate.send("inventory.updated", new InventoryEvent(order.getProductId(), inventory.getQuantity()));

        return order;
    }
}
