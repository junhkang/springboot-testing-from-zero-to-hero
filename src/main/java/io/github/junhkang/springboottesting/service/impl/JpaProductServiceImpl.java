package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.ProductRepository;
import io.github.junhkang.springboottesting.service.ProductService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("jpa")
public class JpaProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public JpaProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    @Override
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
}