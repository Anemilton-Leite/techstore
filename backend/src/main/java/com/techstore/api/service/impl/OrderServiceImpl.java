package com.techstore.api.service.impl;

import com.techstore.api.dto.order.CreateOrderRequest;
import com.techstore.api.dto.order.OrderItemRequest;
import com.techstore.api.dto.order.OrderItemResponse;
import com.techstore.api.dto.order.OrderResponse;
import com.techstore.api.dto.order.OrderSummaryResponse;
import com.techstore.api.entity.Order;
import com.techstore.api.entity.OrderItem;
import com.techstore.api.entity.OrderStatus;
import com.techstore.api.entity.Product;
import com.techstore.api.entity.User;
import com.techstore.api.entity.Address;
import com.techstore.api.exception.BusinessRuleException;
import com.techstore.api.exception.ResourceNotFoundException;
import com.techstore.api.repository.OrderRepository;
import com.techstore.api.repository.ProductRepository;
import com.techstore.api.repository.UserRepository;
import com.techstore.api.repository.AddressRepository;
import com.techstore.api.service.OrderService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderIdempotencyRecovery idempotencyRecovery;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository, AddressRepository addressRepository, OrderIdempotencyRecovery idempotencyRecovery) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.idempotencyRecovery = idempotencyRecovery;
    }

    @Override
    @Transactional
    public OrderResponse create(String customerEmail, CreateOrderRequest request, String idempotencyKey) {
        User customer = userRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new BusinessRuleException("O header Idempotency-Key é obrigatório e deve possuir até 100 caracteres.");
        }

        Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if (existingOrder != null) {
            if (!existingOrder.getCustomer().getId().equals(customer.getId())) {
                throw new BusinessRuleException("Idempotency-Key já utilizada por outro cliente.");
            }
            return toResponse(existingOrder);
        }

        Address address = addressRepository.findByIdAndUserId(request.addressId(), customer.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado."));

        if (request.items().isEmpty()) {
            throw new BusinessRuleException("O pedido deve possuir ao menos um item.");
        }

        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Order order = new Order(customer, OffsetDateTime.now(), OrderStatus.PENDENTE, BigDecimal.ZERO);
        order.setIdempotencyKey(idempotencyKey);
        order.setAddress(address);
        order.setShippingCep(address.getCep());
        order.setShippingStreet(address.getStreet());
        order.setShippingNumber(address.getNumber());
        order.setShippingComplement(address.getComplement());
        order.setShippingNeighborhood(address.getNeighborhood());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + entry.getKey()));
            int quantity = entry.getValue();
            if (product.getStock() < quantity) {
                throw new BusinessRuleException("Estoque insuficiente para o produto: " + product.getName());
            }

            product.setStock(product.getStock() - quantity);
            OrderItem item = new OrderItem(product, quantity, product.getPrice());
            order.addItem(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(total);
        try {
            Order saved = orderRepository.saveAndFlush(order);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            Order concurrentOrder = idempotencyRecovery.findByKey(idempotencyKey).orElseThrow(() -> exception);
            if (!concurrentOrder.getCustomer().getId().equals(customer.getId())) {
                throw new BusinessRuleException("Idempotency-Key já utilizada por outro cliente.");
            }
            return toResponse(concurrentOrder);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> findMine(String customerEmail) {
        User customer = userRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
        return orderRepository.findAllByCustomerIdOrderByOrderDateDesc(customer.getId())
                .stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findMineById(String customerEmail, Long id) {
        User customer = userRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
        return orderRepository.findByIdAndCustomerId(id, customer.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> findAll() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == status) {
            return toResponse(order);
        }

        if (currentStatus == OrderStatus.PAGO) {
            throw new BusinessRuleException("Um pedido pago não pode ter o status alterado.");
        }

        if (currentStatus == OrderStatus.CANCELADO) {
            throw new BusinessRuleException("Um pedido cancelado não pode ser reaberto.");
        }

        if (currentStatus == OrderStatus.PENDENTE && status == OrderStatus.PAGO) {
            order.setStatus(OrderStatus.PAGO);
            return toResponse(orderRepository.save(order));
        }

        if (currentStatus == OrderStatus.PENDENTE && status == OrderStatus.CANCELADO) {
            restoreStock(order);
            order.setStatus(OrderStatus.CANCELADO);
            return toResponse(orderRepository.save(order));
        }

        throw new BusinessRuleException("Transição de status não permitida.");
    }

    private void restoreStock(Order order) {
        order.getItems().forEach(item -> {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + item.getProduct().getId()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        });
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new OrderItemResponse(
                    item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                    item.getQuantity(), item.getUnitPrice(), subtotal);
        }).toList();

        return new OrderResponse(
            order.getId(), order.getCustomer().getId(), order.getAddress() == null ? null : order.getAddress().getId(),
            order.getShippingCep(), order.getShippingStreet(), order.getShippingNumber(), order.getShippingComplement(),
            order.getShippingNeighborhood(), order.getShippingCity(), order.getShippingState(), order.getOrderDate(),
                order.getStatus().name(), order.getTotalAmount(), items);
    }

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(), order.getCustomer().getId(), order.getOrderDate(),
                order.getStatus().name(), order.getTotalAmount());
    }
}
