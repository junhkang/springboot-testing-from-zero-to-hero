package io.github.junhkang.springboottesting.service.impl;


import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.domain.ProductDTO;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.ProductMapper;
import io.github.junhkang.springboottesting.service.ProductService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("mybatis")
public class MyBatisProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    public MyBatisProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public List<Product> getAllProducts() {
        return productMapper.findAll().stream()
                .map(dto -> {
                    Product product = new Product();
                    product.setId(dto.getId());
                    product.setName(dto.getName());
                    product.setDescription(dto.getDescription());
                    product.setPrice(dto.getPrice());
                    product.setStock(dto.getStock());
                    return product;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Product getProductById(Long id) {
        ProductDTO dto = productMapper.findById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Product not found with id " + id);
        }
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        return product;
    }

    @Override
    public Product createProduct(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        productMapper.insert(dto);
        product.setId(dto.getId());
        return product;
    }

}