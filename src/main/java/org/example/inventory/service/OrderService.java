package org.example.inventory.service;

import org.example.inventory.repository.OrderRepository;
import org.example.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.example.inventory.dto.CreateOrderRequest;
import org.example.inventory.model.Order;
import org.springframework.transaction.annotation.Transactional;
import org.example.inventory.dto.OrderItemRequest;
import org.example.inventory.model.Product;
import org.example.inventory.exception.ProductNotFoundException;
import org.example.inventory.exception.InsufficientStockException;
import org.example.inventory.dto.OrderResponse;
import org.example.inventory.dto.OrderItemResponse;
import org.example.inventory.exception.OrderNotFoundException;

import java.util.List;
import java.util.Comparator;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = Order.create();
        List<OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(OrderItemRequest::productId))
                .toList();

        for (OrderItemRequest itemRequest : sortedItems)  {
            Product product = productRepository.findByIdForUpdate(itemRequest.productId())
                    .orElseThrow(() ->
                            new ProductNotFoundException(itemRequest.productId())
                    );
            int requestedQuantity = itemRequest.quantity();

            if (product.getQuantity() < requestedQuantity) {
                throw new InsufficientStockException(
                        product.getId(),
                        requestedQuantity,
                        product.getQuantity()
                );
            }
            product.setQuantity(product.getQuantity() - requestedQuantity);
            order.addItem(product, requestedQuantity);
        }
        Order savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.calculateSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getTotalAmount(),
                items
        );
    }
}