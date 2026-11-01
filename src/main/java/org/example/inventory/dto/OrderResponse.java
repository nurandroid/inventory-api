package org.example.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Оформленный заказ с позициями и итоговой суммой")
public record OrderResponse(
        @Schema(description = "ID заказа", example = "1")
        Long id,

        @Schema(description = "Время создания заказа", example = "2026-10-31T14:00:00Z")
        Instant createdAt,

        @Schema(description = "Общая стоимость всех позиций заказа", example = "49.80")
        BigDecimal totalAmount,

        @Schema(description = "Позиции оформленного заказа")
        List<OrderItemResponse> items
) {
}