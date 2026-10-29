package org.example.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Заказ должен содержать хотя бы одну позицию")
        List<@NotNull(message = "Позиция заказа не должна быть null")
        @Valid OrderItemRequest> items
) {
}