package com.vmc.service;

import com.vmc.model.Order;
import com.vmc.model.OrderDtos.PlaceOrderRequest;
import com.vmc.model.OrderDtos.PlaceOrderResponse;
import com.vmc.model.OrderItem;
import com.vmc.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core order business logic — exactly like any Spring Boot @Service.
 * Saves to DB, then triggers Lambda notification via SQS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository    orderRepository;
    private final SqsPublisherService sqsPublisher;

    /**
     * Places a new order:
     *  1. Validates + saves to DB
     *  2. Publishes to SQS → triggers Lambda → customer gets Email + SMS
     */
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {
        log.info("Placing order for customerId={}", request.getCustomerId());

        // ── 1. Build Order entity ─────────────────────────────────────────────
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .currency("INR")
                .status(Order.OrderStatus.PLACED)
                .estimatedDelivery("3-5 business days")
                .build();

        // ── 2. Map items + calculate total ────────────────────────────────────
        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> OrderItem.builder()
                        .order(order)
                        .productId(itemReq.getProductId())
                        .productName(itemReq.getProductName())
                        .quantity(itemReq.getQuantity())
                        .price(itemReq.getPrice())
                        .build())
                .toList();

        double total = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        order.setItems(items);
        order.setTotalAmount(total);

        // ── 3. Save to database ───────────────────────────────────────────────
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved | orderId={} | total=₹{}", savedOrder.getOrderId(), total);

        // ── 4. Publish to SQS (Lambda picks up → Email + SMS) ─────────────────
        //    Fire-and-forget: SQS publish failure does NOT roll back the order
        sqsPublisher.publishOrderEvent(savedOrder);

        // ── 5. Return response ────────────────────────────────────────────────
        return PlaceOrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .status(savedOrder.getStatus().name())
                .totalAmount(savedOrder.getTotalAmount())
                .currency(savedOrder.getCurrency())
                .estimatedDelivery(savedOrder.getEstimatedDelivery())
                .message("Order placed successfully! You will receive a confirmation email and SMS.")
                .createdAt(savedOrder.getCreatedAt().toString())
                .build();
    }

    /**
     * Fetch all orders for a customer.
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}
