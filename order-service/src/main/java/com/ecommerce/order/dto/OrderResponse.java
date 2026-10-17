package com.ecommerce.order.dto;

public class OrderResponse {

    private Long productId;
    private String productName;
    private int quantity;
    private double pricePerItem;
    private double totalAmount;
    private String status;

    public OrderResponse() {
    }

    public OrderResponse(Long productId, String productName, int quantity,
                         double pricePerItem, double totalAmount, String status) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getPricePerItem() { return pricePerItem; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
}
