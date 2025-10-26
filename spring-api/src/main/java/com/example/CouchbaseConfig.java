package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;

@Configuration
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    @Override
    public String getConnectionString() {
        return System.getenv().getOrDefault("COUCHBASE_HOST", "localhost");
    }

    @Override
    public String getUserName() {
        return System.getenv().getOrDefault("COUCHBASE_USERNAME", "Administrator");
    }

    @Override
    public String getPassword() {
        return System.getenv().getOrDefault("COUCHBASE_PASSWORD", "password");
    }

    @Override
    public String getBucketName() {
        return "inventory"; // Replace with your bucket name
    }
}