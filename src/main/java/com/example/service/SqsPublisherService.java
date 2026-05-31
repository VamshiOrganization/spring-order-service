package com.example.service;

import com.example.model.Order;
import com.example.model.OrderDtos;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes order events to SQS.
 * SQS → triggers Lambda → sends Email + SMS.
 *
 * Uses Spring Cloud AWS SqsTemplate — very similar to JmsTemplate or KafkaTemplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqsPublisherService {

    // ✅ Injected by Spring Cloud AWS — no manual client setup needed
    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.order-notification-queue}")
    private String queueName;

    /**
     * Sends an order event to SQS.
     * Lambda will pick it up and send Email + SMS to the customer.
     *
     * @param order The saved order entity
     */
    public void publishOrderEvent(Order order) {
        log.info("Publishing order event to SQS | orderId={} | queue={}", order.getOrderId(), queueName);

        try {
            // ✅ SqsTemplate serializes this Map to JSON automatically
            Map<String, Object> event = buildOrderEvent(order);

            sqsTemplate.send(queueName, event);

            log.info("Order event published successfully | orderId={}", order.getOrderId());

        } catch (Exception e) {
            // ⚠️ Don't fail the order placement if SQS publish fails
            // The order is already saved in DB — notification can be retried separately
            log.error("Failed to publish order event to SQS | orderId={} | error={}",
                    order.getOrderId(), e.getMessage(), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> buildOrderEvent(Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId",           order.getOrderId());
        event.put("customerId",        order.getCustomerId());
        event.put("customerName",      order.getCustomerName());
        event.put("customerEmail",     order.getCustomerEmail());
        event.put("customerPhone",     order.getCustomerPhone());
        event.put("totalAmount",       order.getTotalAmount());
        event.put("currency",          order.getCurrency());
        event.put("status",            order.getStatus().name());
        event.put("deliveryAddress",   order.getDeliveryAddress());
        event.put("estimatedDelivery", order.getEstimatedDelivery());
        event.put("createdAt",         LocalDateTime.now().toString());

        // Map order items
        var items = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productName", item.getProductName());
                    itemMap.put("quantity",    item.getQuantity());
                    itemMap.put("price",       item.getPrice());
                    return itemMap;
                })
                .toList();
        event.put("items", items);

        return event;
    }
}
