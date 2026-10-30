package org.example.inventory.controller;

import org.example.inventory.exception.GlobalExceptionHandler;
import org.example.inventory.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import org.example.inventory.dto.CreateOrderRequest;
import org.example.inventory.dto.OrderItemRequest;
import org.example.inventory.dto.OrderItemResponse;
import org.example.inventory.dto.OrderResponse;
import org.example.inventory.exception.ProductNotFoundException;
import org.example.inventory.exception.InsufficientStockException;
import org.example.inventory.exception.OrderNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;



@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @org.junit.jupiter.api.BeforeAll
    static void configureJsonPath() {
        com.jayway.jsonpath.Configuration.setDefaults(
                new com.jayway.jsonpath.Configuration.Defaults() {
                    @Override
                    public com.jayway.jsonpath.spi.json.JsonProvider jsonProvider() {
                        return new com.jayway.jsonpath.spi.json.Jackson3JsonProvider();
                    }

                    @Override
                    public com.jayway.jsonpath.spi.mapper.MappingProvider mappingProvider() {
                        return new com.jayway.jsonpath.spi.mapper.Jackson3MappingProvider();
                    }

                    @Override
                    public java.util.Set<com.jayway.jsonpath.Option> options() {
                        return java.util.EnumSet.noneOf(com.jayway.jsonpath.Option.class);
                    }
                }
        );
    }
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void create_shouldReturn400WhenOrderIsEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.items").value(
                        "Заказ должен содержать хотя бы одну позицию"
                ));

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturn400WhenQuantityIsZero() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "productId": 1,
                                  "quantity": 0
                                }
                              ]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['items[0].quantity']").value(
                        "Количество товара должно быть больше нуля"
                ));

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturn201WithCreatedOrder() throws Exception {
        // Подготовка
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 2))
        );

        OrderResponse response = new OrderResponse(
                10L,
                Instant.parse("2026-10-30T08:00:00Z"),
                new BigDecimal("25.00"),
                List.of(new OrderItemResponse(
                        1L,
                        "Клавиатура",
                        2,
                        new BigDecimal("12.50"),
                        new BigDecimal("25.00")
                ))
        );

        when(orderService.create(request)).thenReturn(response);

        // Действие и проверка HTTP-ответа
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "productId": 1,
                                  "quantity": 2
                                }
                              ]
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-30T08:00:00Z"))
                .andExpect(jsonPath("$.totalAmount").value(25.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Клавиатура"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.5))
                .andExpect(jsonPath("$.items[0].subtotal").value(25.0));

        verify(orderService).create(request);
    }

    @Test
    void create_shouldReturn404WhenProductNotFound() throws Exception {
        // Подготовка
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(99L, 2))
        );

        when(orderService.create(request))
                .thenThrow(new ProductNotFoundException(99L));

        // Действие и проверка ответа
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "productId": 99,
                                  "quantity": 2
                                }
                              ]
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Товар с id 99 не найден"
                ));

        verify(orderService).create(request);
    }

    @Test
    void create_shouldReturn409WhenStockIsInsufficient() throws Exception {
        // Подготовка
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 2))
        );

        when(orderService.create(request))
                .thenThrow(new InsufficientStockException(1L, 2, 1));

        // Действие и проверка ответа
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "productId": 1,
                                  "quantity": 2
                                }
                              ]
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Недостаточно товара с id 1: запрошено 2, доступно 1"
                ));

        verify(orderService).create(request);
    }

    @Test
    void findById_shouldReturn200WithOrder() throws Exception {
        // Подготовка
        OrderResponse response = new OrderResponse(
                10L,
                Instant.parse("2026-10-30T08:00:00Z"),
                new BigDecimal("25.00"),
                List.of(new OrderItemResponse(
                        1L,
                        "Клавиатура",
                        2,
                        new BigDecimal("12.50"),
                        new BigDecimal("25.00")
                ))
        );

        when(orderService.findById(10L)).thenReturn(response);

        // Действие и проверка ответа
        mockMvc.perform(get("/api/orders/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-30T08:00:00Z"))
                .andExpect(jsonPath("$.totalAmount").value(25.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Клавиатура"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(12.5))
                .andExpect(jsonPath("$.items[0].subtotal").value(25.0));

        verify(orderService).findById(10L);
    }

    @Test
    void findById_shouldReturn404WhenOrderNotFound() throws Exception {
        // Подготовка
        when(orderService.findById(99L))
                .thenThrow(new OrderNotFoundException(99L));

        // Действие и проверка ответа
        mockMvc.perform(get("/api/orders/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Заказ с id 99 не найден"
                ));

        verify(orderService).findById(99L);
    }

    @Test
    void create_shouldReturn400WhenProductIdIsZero() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "productId": 0,
                                  "quantity": 2
                                }
                              ]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['items[0].productId']").value(
                        "Идентификатор товара должен быть положительным"
                ));

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturn400WhenQuantityIsMissing() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "productId": 1
                                }
                              ]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['items[0].quantity']").value(
                        "Количество товара обязательно"
                ));

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturn400WhenProductIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "items": [
                                {
                                  "quantity": 2
                                }
                              ]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['items[0].productId']").value(
                        "Идентификатор товара обязателен"
                ));

        verifyNoInteractions(orderService);
    }
}