package org.example.inventory.service;

import org.example.inventory.dto.CreateOrderRequest;
import org.example.inventory.dto.OrderItemRequest;
import org.example.inventory.dto.OrderResponse;
import org.example.inventory.model.Product;
import org.example.inventory.repository.OrderRepository;
import org.example.inventory.repository.ProductRepository;
import org.example.inventory.exception.InsufficientStockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;



@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void prepareDatabase() throws SQLException {
        // Проверяем базу до удаления данных
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("inventory_test_db", connection.getCatalog());
        }

        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void create_shouldPersistOrderAndDecreaseStock() {
        // Подготовка
        Product product = new Product();
        product.setName("Клавиатура");
        product.setQuantity(10);
        product.setPrice(new BigDecimal("12.50"));

        Product savedProduct = productRepository.saveAndFlush(product);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(savedProduct.getId(), 2))
        );

        // Действие: транзакция сервиса завершается при возврате
        OrderResponse created = orderService.create(request);

        // Повторное чтение из PostgreSQL
        Product reloadedProduct = productRepository
                .findById(savedProduct.getId())
                .orElseThrow();

        OrderResponse reloadedOrder = orderService.findById(created.id());

        // Проверка
        assertNotNull(created.id());
        assertNotNull(reloadedOrder.createdAt());
        assertEquals(8, reloadedProduct.getQuantity());
        assertEquals(new BigDecimal("25.00"), reloadedOrder.totalAmount());
        assertEquals(1, reloadedOrder.items().size());
        assertEquals(1L, orderRepository.count());
    }

    @Test
    void create_shouldRollbackWhenSecondProductHasInsufficientStock() {
        // Подготовка: первый товар сохраняем раньше второго,
        // чтобы его id был меньше и сервис обработал его первым
        Product keyboard = new Product();
        keyboard.setName("Клавиатура");
        keyboard.setQuantity(10);
        keyboard.setPrice(new BigDecimal("12.50"));
        Product savedKeyboard = productRepository.saveAndFlush(keyboard);

        Product mouse = new Product();
        mouse.setName("Мышь");
        mouse.setQuantity(1);
        mouse.setPrice(new BigDecimal("7.30"));
        Product savedMouse = productRepository.saveAndFlush(mouse);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new OrderItemRequest(savedKeyboard.getId(), 2),
                        new OrderItemRequest(savedMouse.getId(), 2)
                )
        );

        // Действие: второй товар вызывает отказ и откат транзакции
        assertThrows(
                InsufficientStockException.class,
                () -> orderService.create(request)
        );

        // Повторное чтение после отката
        Product reloadedKeyboard = productRepository
                .findById(savedKeyboard.getId())
                .orElseThrow();

        Product reloadedMouse = productRepository
                .findById(savedMouse.getId())
                .orElseThrow();

        assertEquals(10, reloadedKeyboard.getQuantity());
        assertEquals(1, reloadedMouse.getQuantity());
        assertEquals(0L, orderRepository.count());
    }

    @Test
    void create_shouldAllowOnlyOneOrderForLastUnit() throws Exception {
        // Подготовка: последняя единица товара
        Product product = new Product();
        product.setName("Клавиатура");
        product.setQuantity(1);
        product.setPrice(new BigDecimal("12.50"));

        Product savedProduct = productRepository.saveAndFlush(product);

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(savedProduct.getId(), 1))
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> placeOrder = () -> {
            ready.countDown();

            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Не получен сигнал старта");
            }

            try {
                orderService.create(request);
                return true;
            } catch (InsufficientStockException exception) {
                return false;
            }
        };

        Future<Boolean> first = null;
        Future<Boolean> second = null;

        try {
            first = executor.submit(placeOrder);
            second = executor.submit(placeOrder);

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Оба потока должны подготовиться к запуску"
            );

            // Разрешаем обоим потокам вызвать сервис
            start.countDown();

            boolean firstSucceeded = first.get(10, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(10, TimeUnit.SECONDS);

            int successfulOrders = (firstSucceeded ? 1 : 0)
                    + (secondSucceeded ? 1 : 0);

            assertEquals(1, successfulOrders);

            Product reloadedProduct = productRepository
                    .findById(savedProduct.getId())
                    .orElseThrow();

            assertEquals(0, reloadedProduct.getQuantity());
            assertEquals(1L, orderRepository.count());
        } finally {
            start.countDown();

            if (first != null) {
                first.cancel(true);
            }
            if (second != null) {
                second.cancel(true);
            }

            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Рабочие потоки должны завершиться"
            );
        }
    }
}