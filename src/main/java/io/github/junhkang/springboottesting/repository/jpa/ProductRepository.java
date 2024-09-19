package io.github.junhkang.springboottesting.repository.jpa;

import io.github.junhkang.springboottesting.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}