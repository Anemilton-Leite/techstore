package com.techstore.api.controller;

import com.techstore.api.dto.order.CreateOrderRequest;
import com.techstore.api.dto.order.OrderResponse;
import com.techstore.api.dto.order.OrderSummaryResponse;
import com.techstore.api.dto.order.UpdateOrderStatusRequest;
import com.techstore.api.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Pedidos")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria pedido para o cliente autenticado")
    public OrderResponse create(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return service.create(authentication.getName(), request, idempotencyKey);
    }

    @GetMapping("/me")
    @Operation(summary = "Lista pedidos do cliente autenticado")
    public List<OrderSummaryResponse> findMine(Authentication authentication) {
        return service.findMine(authentication.getName());
    }

    @GetMapping("/me/{id}")
    @Operation(summary = "Busca um pedido do cliente autenticado")
    public OrderResponse findMineById(Authentication authentication, @PathVariable Long id) {
        return service.findMineById(authentication.getName(), id);
    }

    @GetMapping("/admin")
    @Operation(summary = "Lista todos os pedidos (admin)")
    public List<OrderSummaryResponse> findAll() {
        return service.findAll();
    }

    @PatchMapping("/admin/{id}/status")
    @Operation(summary = "Altera status do pedido (admin)")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
