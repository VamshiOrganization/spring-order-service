package com.vmc.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTOs for REST API — keeps entity separate from API contract.
 */
public class OrderDtos {

    // ── Request DTO (incoming from client) ────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceOrderRequest {

        @NotBlank(message = "Customer ID is required")
        private String customerId;

        @NotBlank(message = "Customer name is required")
        private String customerName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String customerEmail;

        @Pattern(regexp = "^\\+91[0-9]{10}$", message = "Phone must be in format +91XXXXXXXXXX")
        private String customerPhone;

        @NotBlank(message = "Delivery address is required")
        private String deliveryAddress;

        @NotEmpty(message = "Order must have at least one item")
        @Valid
        private List<OrderItemRequest> items;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderItemRequest {
            @NotBlank private String productId;
            @NotBlank private String productName;
            @Min(1)   private int quantity;
            @Min(0)   private double price;
        }
    }

    // ── Response DTO (sent back to client) ────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceOrderResponse {
        private String  orderId;
        private String  status;
        private double  totalAmount;
        private String  currency;
        private String  estimatedDelivery;
        private String  message;
        private String  createdAt;
    }
}
