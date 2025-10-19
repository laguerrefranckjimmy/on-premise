package com.example;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends CouchbaseRepository<Inventory, String> {
    // You can add custom queries if needed
}
