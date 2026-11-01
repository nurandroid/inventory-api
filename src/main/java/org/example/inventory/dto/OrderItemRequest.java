package org.example.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Товар и количество для одной позиции заказа")

public record OrderItemRequest(
        @Schema(description = "ID существующего товара из каталога", example = "3")
        @NotNull(message = "Идентификатор товара обязателен")
        @Positive(message = "Идентификатор товара должен быть положительным")
        Long productId,

        @Schema(description = "Количество заказываемых единиц товара", example = "1")
        @NotNull(message = "Количество товара обязательно")
        @Positive(message = "Количество товара должно быть больше нуля")
        Integer quantity
) {
}