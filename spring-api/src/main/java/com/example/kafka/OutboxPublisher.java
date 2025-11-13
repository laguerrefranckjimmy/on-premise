package com.example.kafka;

import com.example.repositories.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // runs every 2 seconds and flushes unsent events
    @Scheduled(fixedDelay = 2000)
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findBySentFalse();

        for (OutboxEvent e : pending) {
            try {
                ProducerRecord<String, String> rec =
                        new ProducerRecord<>(e.getTopic(), e.getPayload());

                if (e.getCorrelationId() != null) {
                    rec.headers().add(new RecordHeader(
                            "X-Correlation-ID",
                            e.getCorrelationId().getBytes(StandardCharsets.UTF_8)
                    ));
                }

                kafkaTemplate.send(rec);
                e.setSent(true);
                outboxRepository.save(e);

                logger.info("Published outbox event {} to topic {}", e.getId(), e.getTopic());
            } catch (Exception ex) {
                logger.error("Failed to publish outbox event {}: {}", e.getId(), ex.getMessage());
            }
        }
    }
}
