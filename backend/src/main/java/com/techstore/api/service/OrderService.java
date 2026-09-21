package com.techstore.api.service;

import com.techstore.api.dto.order.CreateOrderRequest;
import com.techstore.api.dto.order.OrderResponse;
import com.techstore.api.dto.order.OrderSummaryResponse;
import com.techstore.api.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponse create(String customerEmail, CreateOrderRequest request, String idempotencyKey);
    List<OrderSummaryResponse> findMine(String customerEmail);
    OrderResponse findMineById(String customerEmail, Long id);
    List<OrderSummaryResponse> findAll();
    OrderResponse updateStatus(Long id, OrderStatus status);
}
