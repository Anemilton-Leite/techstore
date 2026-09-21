package com.techstore.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "shipping_cep", length = 8)
    private String shippingCep;
    @Column(name = "shipping_street", length = 120)
    private String shippingStreet;
    @Column(name = "shipping_number", length = 20)
    private String shippingNumber;
    @Column(name = "shipping_complement", length = 120)
    private String shippingComplement;
    @Column(name = "shipping_neighborhood", length = 100)
    private String shippingNeighborhood;
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;
    @Column(name = "shipping_state", length = 2)
    private String shippingState;

    @Column(name = "order_date", nullable = false)
    private OffsetDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(User customer, OffsetDateTime orderDate, OrderStatus status, BigDecimal totalAmount) {
        this.customer = customer;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
    public String getShippingCep() { return shippingCep; }
    public void setShippingCep(String shippingCep) { this.shippingCep = shippingCep; }
    public String getShippingStreet() { return shippingStreet; }
    public void setShippingStreet(String shippingStreet) { this.shippingStreet = shippingStreet; }
    public String getShippingNumber() { return shippingNumber; }
    public void setShippingNumber(String shippingNumber) { this.shippingNumber = shippingNumber; }
    public String getShippingComplement() { return shippingComplement; }
    public void setShippingComplement(String shippingComplement) { this.shippingComplement = shippingComplement; }
    public String getShippingNeighborhood() { return shippingNeighborhood; }
    public void setShippingNeighborhood(String shippingNeighborhood) { this.shippingNeighborhood = shippingNeighborhood; }
    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }
    public String getShippingState() { return shippingState; }
    public void setShippingState(String shippingState) { this.shippingState = shippingState; }
    public OffsetDateTime getOrderDate() { return orderDate; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public List<OrderItem> getItems() { return items; }
}
