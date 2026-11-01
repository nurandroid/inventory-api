package org.example.inventory.controller;

import org.example.inventory.service.OrderService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.example.inventory.dto.CreateOrderRequest;
import org.example.inventory.dto.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Tag(
        name = "Заказы",
        description = "Оформление и просмотр заказов"
)

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Создать заказ",
            description = "Принимает товары и их количество. Цены берутся из каталога. "
                    + "При достаточных остатках сохраняет заказ и списывает товары. "
                    + "При нехватке хотя бы одного товара отклоняет весь заказ."
    )

    @ApiResponse(
            responseCode = "201",
            description = "Заказ создан, складские остатки уменьшены"
    )

    @ApiResponse(
            responseCode = "409",
            description = "Недостаточно товара на складе; весь заказ отклонён",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"message\": \"Недостаточно товара на складе\"}"
                    )
            )
    )

    @ApiResponse(
            responseCode = "404",
            description = "Один из товаров заказа не найден",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"message\": \"Товар не найден\"}"
                    )
            )
    )

    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации данных заказа",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"items\": \"Сообщение об ошибке в списке позиций заказа\"}"
                    )
            )
    )

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Получить заказ по ID")

    @ApiResponse(
            responseCode = "200",
            description = "Заказ найден"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Заказ не найден",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"message\": \"Заказ не найден\"}"
                    )
            )
    )

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }
}