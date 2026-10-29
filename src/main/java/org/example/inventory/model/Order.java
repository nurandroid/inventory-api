package org.example.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.Instant;
import jakarta.persistence.PrePersist;
import java.math.BigDecimal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();


    protected Order() {
    }
    public static Order create() {
        Order order = new Order();
        order.totalAmount = BigDecimal.ZERO;
        return order;
    }
    public void addItem(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Количество товара в заказе должно быть больше нуля"
            );
        }

        OrderItem item = new OrderItem(this, product, quantity);
        items.add(item);
        totalAmount = totalAmount.add(item.calculateSubtotal());
    }

    public Long getId() {
        return id;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}