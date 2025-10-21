package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RealTimeVertxServer extends AbstractVerticle {

    private Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        // WebSocket server
        vertx.createHttpServer().webSocketHandler(ws -> {
            // Accept only specific path
            if (!"/ws".equals(ws.path())) {
                ws.reject();
                return;
            }
            String id = ws.textHandlerID();
            clients.put(id, ws);
            ws.closeHandler(v -> clients.remove(id));
        }).listen(8081, ar -> {
            if (ar.succeeded()) {
                System.out.println("Vert.x WebSocket server listening on 8081 (path /ws)");
            } else {
                ar.cause().printStackTrace();
            }
        });

        // Kafka consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "realtime-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, props);
        consumer.subscribe((Set<String>) Arrays.asList("order.created", "inventory.updated"));

        consumer.handler(record -> {
            String value = record.value();
            // Broadcast event to all connected React clients
            for (ServerWebSocket ws : clients.values()) {
                if (!ws.isClosed()) {
                    ws.writeTextMessage(value);
                }
            }
        });
    }
}