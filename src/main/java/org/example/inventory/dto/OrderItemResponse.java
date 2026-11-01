package org.example.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Позиция оформленного заказа")
public record OrderItemResponse(
        @Schema(description = "ID заказанного товара", example = "3")
        Long productId,

        @Schema(description = "Название товара", example = "Mouse")
        String productName,

        @Schema(description = "Количество купленных единиц", example = "2")
        int quantity,

        @Schema(description = "Цена единицы на момент оформления заказа", example = "24.90")
        BigDecimal unitPrice,

        @Schema(description = "Стоимость позиции: количество × цена единицы", example = "49.80")
        BigDecimal subtotal
) {
}