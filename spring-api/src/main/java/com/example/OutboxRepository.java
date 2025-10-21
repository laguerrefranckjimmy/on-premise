package com.example;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends CouchbaseRepository<OutboxEvent, String> {
}