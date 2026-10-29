package org.example.inventory.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;


import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Название товара не должно быть пустым")
    private String name;
    @PositiveOrZero(message = "Остаток товара не должен быть отрицательным")
    private int quantity;
    @NotNull(message = "Цена товара обязательна")
    @DecimalMin(value = "0.00", inclusive = false,
            message = "Цена товара должна быть больше нуля")
    @Digits(integer = 10, fraction = 2,
            message = "Цена должна содержать не более 10 цифр до запятой и 2 после")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    public Product() {
    }
    public Product(Long id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
