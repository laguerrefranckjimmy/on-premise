package com.example.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaHealthConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaHealthConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public HealthIndicator kafkaHealthIndicator() {
        return () -> {
            // Build AdminClient props from your existing spring.kafka.* settings
            Map<String, Object> adminProps = new HashMap<>(kafkaProperties.buildAdminProperties());

            try (AdminClient client = AdminClient.create(adminProps)) {
                DescribeClusterResult result = client.describeCluster();
                String clusterId = result.clusterId().get(5, TimeUnit.SECONDS);

                return Health.up()
                        .withDetail("clusterId", clusterId)
                        .withDetail("nodes", result.nodes().get(5, TimeUnit.SECONDS).toString())
                        .build();
            } catch (Exception e) {
                return Health.down(e).build();
            }
        };
    }
}