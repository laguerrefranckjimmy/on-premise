package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealTimeVertxServer extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(RealTimeVertxServer.class);

    private final Map<String, ServerWebSocket> clients = new ConcurrentHashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {

        vertx.createHttpServer()
                .webSocketHandler(ws -> {
                    // Only accept /ws
                    if (!"/ws".equals(ws.path())) {
                        ws.reject();
                        return;
                    }

                    String id = UUID.randomUUID().toString();
                    clients.put(id, ws);

                    if (log.isDebugEnabled()) {
                        log.debug("WebSocket client connected: {}", id);
                    }

                    ws.closeHandler(v -> {
                        clients.remove(id);
                        if (log.isDebugEnabled()) {
                            log.debug("WebSocket client disconnected: {}", id);
                        }
                    });
                })
                .listen(8081, ar -> {
                    if (ar.succeeded()) {
                        log.info("Vert.x WebSocket server listening on 8081 (path /ws)");

                        // Initialize Kafka consumer off the event loop to avoid blocked-thread warnings
                        vertx.executeBlocking(promise -> {
                            try {
                                initKafkaConsumer();
                                promise.complete();
                            } catch (Exception e) {
                                promise.fail(e);
                            }
                        }, res -> {
                            if (res.succeeded()) {
                                log.info("Kafka consumer initialization completed");
                            } else {
                                log.error("Kafka consumer initialization failed", res.cause());
                                // Optional: decide if you want to shut down Vert.x if Kafka fails to init
                            }
                        });

                        startPromise.complete();
                    } else {
                        log.error("Failed to start Vert.x HTTP server", ar.cause());
                        startPromise.fail(ar.cause());
                    }
                });
    }

    private void initKafkaConsumer() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "d4bp18ce45lfjaeu54kg.any.us-east-1.mpx.prd.cloud.redpanda.com:9092");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", "realtime-group");
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "true");

        String username = getEnvOrThrow("KAFKA_USERNAME");
        String password = getEnvOrThrow("KAFKA_PASSWORD");

        // Redpanda Cloud security
        config.put("security.protocol", "SASL_SSL");
        config.put("sasl.mechanism", "SCRAM-SHA-256");
        config.put("sasl.jaas.config",
                String.format(
                        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
                        username, password
                )
        );

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);

        consumer.subscribe(Set.of("inventory.updated", "order.created"), ar -> {
            if (ar.succeeded()) {
                log.info("Kafka consumer subscribed to topics: inventory.updated, order.created");
            } else {
                log.error("Kafka subscription failed", ar.cause());
            }
        });

        consumer.handler((KafkaConsumerRecord<String, String> record) -> {
            String topic = record.topic();
            String value = record.value();

            if (log.isDebugEnabled()) {
                log.debug("Received from Kafka [{}]: {}", topic, value);
            }

            // Fan-out to connected WebSocket clients
            for (ServerWebSocket ws : clients.values()) {
                if (!ws.isClosed()) {
                    ws.writeTextMessage(value);
                }
            }
        });

        consumer.exceptionHandler(err -> log.error("Kafka consumer error", err));
    }


    private String getEnvOrThrow(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return v;
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new RealTimeVertxServer(), res -> {
            if (res.succeeded()) {
                log.info("RealTimeVertxServer deployed, id={}", res.result());
            } else {
                log.error("Failed to deploy RealTimeVertxServer", res.cause());
            }
        });
    }
}
