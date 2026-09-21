package com.techstore.api.repository;

import com.techstore.api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByCustomerIdOrderByOrderDateDesc(Long customerId);
    List<Order> findAllByOrderByOrderDateDesc();
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
    @Query("select order from Order order where order.id = :id and order.customer.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
