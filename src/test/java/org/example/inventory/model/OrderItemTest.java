package org.example.inventory.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTest {

    @Test
    void calculateSubtotal_shouldMultiplyUnitPriceByQuantity() {
        // Setup
        Product product = new Product(1L, "Клавиатура", 10);
        product.setPrice(new BigDecimal("12.50"));

        Order order = Order.create();
        OrderItem item = new OrderItem(order, product, 2);

        // Action
        BigDecimal actual = item.calculateSubtotal();

        // Check the result
        assertEquals(new BigDecimal("25.00"), actual);
    }

    @Test
    void calculateSubtotal_shouldKeepOriginalPriceWhenProductPriceChanges() {
        // Setup: create an order item at a price of 12.50
        Product product = new Product(1L, "Клавиатура", 10);
        product.setPrice(new BigDecimal("12.50"));

        Order order = Order.create();
        OrderItem item = new OrderItem(order, product, 2);

        // Action: change the catalog price
        product.setPrice(new BigDecimal("20.00"));

        // Check: the order item keeps the original price
        assertEquals(new BigDecimal("12.50"), item.getUnitPrice());
        assertEquals(new BigDecimal("25.00"), item.calculateSubtotal());
    }
}