package com.example;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.UUID;

@Document
public class Order {

    @Id
    private String orderId;
    private String productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private String customerName;
    private String customerEmail;
    private String storeId;
    private String storeName;
    private LocalDateTime orderDate;
    private String status; // e.g., "CREATED", "CONFIRMED", "SHIPPED"

    public Order() {
        this.orderId = UUID.randomUUID().toString();
        this.orderDate = LocalDateTime.now();
        this.status = "CREATED";
    }

    public Order(String productId, String productName, int quantity, double unitPrice,
                 String customerName, String customerEmail, String storeId, String storeName) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice * quantity;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.storeId = storeId;
        this.storeName = storeName;
    }

    // --- Getters and Setters ---
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = this.unitPrice * this.quantity;
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.totalPrice = this.unitPrice * this.quantity;
    }

    public double getTotalPrice() { return totalPrice; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
