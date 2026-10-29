package org.example.inventory.repository;

import org.example.inventory.model.Product;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    public  Product save(Product product) {
        if (product.getId() == null) {
            product.setId(counter.incrementAndGet());
        }

        products.put(product.getId(), product);
        return product;
    }

    public boolean existsById(Long id) {
        return products.containsKey(id);
    }

    public void deleteById(Long id) {
        products.remove(id);
    }

}
