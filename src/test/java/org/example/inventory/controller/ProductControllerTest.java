package org.example.inventory.controller;

import org.example.inventory.exception.GlobalExceptionHandler;
import org.example.inventory.model.Product;
import org.example.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void create_shouldAcceptNameWith255Characters() throws Exception {
        String name = "A".repeat(255);
        when(productService.create(any(Product.class)))
                .thenReturn(savedProduct(name));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(name)))
                .andExpect(status().isCreated())
                .andExpect(content().json(productJson(name)));

        verify(productService).create(argThat(
                product -> name.equals(product.getName())
        ));
    }

    @Test
    void create_shouldRejectNameWith256Characters() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("A".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                             "message": "Ошибка проверки данных",
                             "errors": {
                                 "name": "Название товара не должно превышать 255 символов"
                             }
                         }
                        """));

        verifyNoInteractions(productService);
    }

    @Test
    void update_shouldAcceptNameWith255Characters() throws Exception {
        String name = "A".repeat(255);
        when(productService.update(eq(1L), any(Product.class)))
                .thenReturn(Optional.of(savedProduct(name)));

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(name)))
                .andExpect(status().isOk())
                .andExpect(content().json(productJson(name)));

        verify(productService).update(eq(1L), argThat(
                product -> name.equals(product.getName())
        ));
    }

    @Test
    void update_shouldRejectNameWith256Characters() throws Exception {
        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("A".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                             "message": "Ошибка проверки данных",
                             "errors": {
                                 "name": "Название товара не должно превышать 255 символов"
                             }
                         }
                        """));

        verifyNoInteractions(productService);
    }

    private static Product savedProduct(String name) {
        Product product = new Product(1L, name, 10);
        product.setPrice(new BigDecimal("24.90"));
        return product;
    }

    private static String productJson(String name) {
        return """
                {
                  "name": "%s",
                  "quantity": 10,
                  "price": 24.90
                }
                """.formatted(name);
    }
}