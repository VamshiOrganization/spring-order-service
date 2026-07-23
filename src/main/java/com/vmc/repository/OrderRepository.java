package com.vmc.repository;

import com.vmc.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository — exactly as you know it.
 * No changes needed here — this is pure Spring Boot.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // Find all orders for a customer
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    // Find by status
    List<Order> findByStatus(Order.OrderStatus status);

    // Find by email
    Optional<Order> findByOrderIdAndCustomerEmail(String orderId, String email);

    // Count orders by customer
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerId = :customerId")
    long countByCustomerId(String customerId);
}
