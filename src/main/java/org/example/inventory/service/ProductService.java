package org.example.inventory.service;

import org.example.inventory.model.Product;
import org.example.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product create(Product product) {
        product.setId(null);
        return productRepository.save(product);
    }

    public Optional<Product> update(Long id, Product product) {
        Optional<Product> existingProduct = productRepository.findById(id);

        if (existingProduct.isEmpty()) {
            return Optional.empty();
        }

        Product existing = existingProduct.get();
        existing.setName(product.getName());
        existing.setQuantity(product.getQuantity());

        return Optional.of(productRepository.save(existing));
    }
    public boolean deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            return false;
        }
        productRepository.deleteById(id);
        return true;
    }

}