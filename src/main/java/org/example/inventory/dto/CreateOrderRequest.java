package org.example.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Данные для оформления заказа. Цены и итоговую сумму рассчитывает сервер.")

public record CreateOrderRequest(
        @Schema(description = "Позиции заказа; список должен содержать хотя бы одну позицию")
        @NotEmpty(message = "Заказ должен содержать хотя бы одну позицию")
        List<@NotNull(message = "Позиция заказа не должна быть null")
        @Valid OrderItemRequest> items
) {
}