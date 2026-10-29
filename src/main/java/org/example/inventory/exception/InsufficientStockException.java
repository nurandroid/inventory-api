package org.example.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(
            Long productId, int requested, int available) {

        super("Недостаточно товара с id " + productId
                + ": запрошено " + requested
                + ", доступно " + available);
    }
}