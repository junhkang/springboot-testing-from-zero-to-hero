package io.github.junhkang.springboottesting.service;


import io.github.junhkang.springboottesting.domain.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product createProduct(Product product);
}