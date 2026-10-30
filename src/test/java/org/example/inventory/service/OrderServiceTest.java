package org.example.inventory.service;

import org.example.inventory.dto.CreateOrderRequest;
import org.example.inventory.dto.OrderItemRequest;
import org.example.inventory.dto.OrderResponse;
import org.example.inventory.model.Order;
import org.example.inventory.model.Product;
import org.example.inventory.repository.OrderRepository;
import org.example.inventory.repository.ProductRepository;
import org.example.inventory.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.inventory.exception.ProductNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_shouldCalculateTotalAndDecreaseStock() {
        // Подготовка
        Product product = new Product(1L, "Клавиатура", 10);
        product.setPrice(new BigDecimal("12.50"));

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 2))
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Действие
        OrderResponse response = orderService.create(request);

        // Проверка
        assertEquals(new BigDecimal("25.00"), response.totalAmount());
        assertEquals(8, product.getQuantity());
        assertEquals(1, response.items().size());

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void create_shouldThrowWhenProductNotFound() {
        // Подготовка
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(99L, 2))
        );

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        // Действие и проверка исключения
        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.create(request)
        );

        // Проверка отсутствия сохранения
        verifyNoInteractions(orderRepository);
    }
    @Test
    void create_shouldThrowWhenStockIsInsufficient() {
        // Подготовка: есть 1 единица, заказываем 2
        Product product = new Product(1L, "Клавиатура", 1);
        product.setPrice(new BigDecimal("12.50"));

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 2))
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        // Действие и проверка исключения
        assertThrows(
                InsufficientStockException.class,
                () -> orderService.create(request)
        );

        // Остаток не изменился, заказ не сохранялся
        assertEquals(1, product.getQuantity());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void create_shouldSumMultipleItemsAndDecreaseBothStocks() {
        // Подготовка
        Product keyboard = new Product(1L, "Клавиатура", 10);
        keyboard.setPrice(new BigDecimal("12.50"));

        Product mouse = new Product(2L, "Мышь", 5);
        mouse.setPrice(new BigDecimal("7.30"));

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 3)
                )
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(keyboard));

        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(mouse));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Действие
        OrderResponse response = orderService.create(request);

        // Проверка
        assertEquals(new BigDecimal("46.90"), response.totalAmount());
        assertEquals(2, response.items().size());
        assertEquals(8, keyboard.getQuantity());
        assertEquals(2, mouse.getQuantity());

        verify(orderRepository).save(any(Order.class));
    }
}