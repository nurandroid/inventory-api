package org.example.inventory.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTest {

    @Test
    void calculateSubtotal_shouldMultiplyUnitPriceByQuantity() {
        // Подготовка
        Product product = new Product(1L, "Клавиатура", 10);
        product.setPrice(new BigDecimal("12.50"));

        Order order = Order.create();
        OrderItem item = new OrderItem(order, product, 2);

        // Действие
        BigDecimal actual = item.calculateSubtotal();

        // Проверка
        assertEquals(new BigDecimal("25.00"), actual);
    }

    @Test
    void calculateSubtotal_shouldKeepOriginalPriceWhenProductPriceChanges() {
        // Подготовка: создаём позицию по цене 12.50
        Product product = new Product(1L, "Клавиатура", 10);
        product.setPrice(new BigDecimal("12.50"));

        Order order = Order.create();
        OrderItem item = new OrderItem(order, product, 2);

        // Действие: меняем цену в каталоге
        product.setPrice(new BigDecimal("20.00"));

        // Проверка: позиция сохранила исходную цену
        assertEquals(new BigDecimal("12.50"), item.getUnitPrice());
        assertEquals(new BigDecimal("25.00"), item.calculateSubtotal());
    }
}