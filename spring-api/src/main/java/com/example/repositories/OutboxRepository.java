package com.example.repositories;

import com.example.kafka.OutboxEvent;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends CouchbaseRepository<OutboxEvent, String> {

    // Only process events that haven't been published yet
    List<OutboxEvent> findBySentFalse();
}
