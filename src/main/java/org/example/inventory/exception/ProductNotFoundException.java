package org.example.inventory.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Товар с id " + productId + " не найден");
    }
}