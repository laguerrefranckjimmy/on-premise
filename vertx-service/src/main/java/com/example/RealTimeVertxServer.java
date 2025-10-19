package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.kafka.client.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RealTimeVertxServer extends AbstractVerticle {

    private Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        // WebSocket server for React clients
        vertx.createHttpServer().webSocketHandler(ws -> {
            clients.put(ws.textHandlerID(), ws);
            ws.closeHandler(v -> clients.remove(ws.textHandlerID()));
        }).listen(8081);

        // Kafka consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "vertx-consumer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, props);
        consumer.subscribe((Set<String>) Arrays.asList("order.created", "inventory.updated"));

        consumer.handler(record -> {
            // Broadcast event to all connected React clients
            for(ServerWebSocket ws : clients.values()) {
                ws.writeTextMessage(record.value());
            }
        });
    }
}
