package org.example.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Instant createdAt,
        BigDecimal totalAmount,
        List<OrderItemResponse> items
) {
}