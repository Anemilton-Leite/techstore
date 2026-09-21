package com.techstore.api.service.impl;

import com.techstore.api.entity.Order;
import com.techstore.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrderIdempotencyRecovery {
    private final OrderRepository orderRepository;

    public OrderIdempotencyRecovery(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Order> findByKey(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey);
    }
}