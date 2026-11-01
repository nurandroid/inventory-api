package org.example.inventory.controller;

import jakarta.validation.Valid;
import org.example.inventory.model.Product;
import org.example.inventory.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;

@Tag(
        name = "Товары",
        description = "Каталог товаров: просмотр, создание, изменение и удаление"
)
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @Operation(
            summary = "Получить список товаров",
            description = "Возвращает все товары каталога с текущими ценами и складскими остатками."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список товаров получен успешно"
    )
    @GetMapping
    public List<Product> findAll() {
        return productService.findAll();
    }

    @Operation(summary = "Получить товар по ID")
    @ApiResponse(
            responseCode = "200",
            description = "Товар найден"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Товар не найден",
            content = @io.swagger.v3.oas.annotations.media.Content
    )

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(product -> ResponseEntity.ok(product))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Создать товар")
    @ApiResponse(
            responseCode = "201",
            description = "Товар создан"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации данных товара",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"name\": \"Сообщение об ошибке в названии товара\"}"
                    )
            )
    )

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        Product savedProduct = productService.create(product);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedProduct);
    }

    @Operation(summary = "Изменить товар")
    @ApiResponse(
            responseCode = "200",
            description = "Товар изменён"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Товар не найден",
            content = @io.swagger.v3.oas.annotations.media.Content
    )

    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации данных товара",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"name\": \"Название товара не должно быть пустым\"}"
                    )
            )
    )

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody Product product) {

        return productService.update(id, product)
                .map(updatedProduct -> ResponseEntity.ok(updatedProduct))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Удалить товар")
    @ApiResponse(
            responseCode = "204",
            description = "Товар удалён",
            content = @io.swagger.v3.oas.annotations.media.Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "Товар не найден",
            content = @io.swagger.v3.oas.annotations.media.Content
    )
    @ApiResponse(
            responseCode = "409",
            description = "Удаление нарушает ограничения данных: например, товар используется в заказе",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "object",
                            additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.TRUE
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            value = "{\"message\": \"Операция нарушает ограничения данных. Возможно, запись используется в связанных данных.\"}"
                    )
            )
    )

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (productService.deleteById(id)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}