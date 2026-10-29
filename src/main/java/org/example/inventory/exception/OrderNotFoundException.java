package org.example.inventory.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("Заказ с id " + orderId + " не найден");
    }
}