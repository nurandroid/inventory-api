package org.example.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull(message = "Идентификатор товара обязателен")
        @Positive(message = "Идентификатор товара должен быть положительным")
        Long productId,

        @NotNull(message = "Количество товара обязательно")
        @Positive(message = "Количество товара должно быть больше нуля")
        Integer quantity
) {
}