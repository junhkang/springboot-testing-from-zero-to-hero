package io.github.junhkang.springboottesting.repository.mybatis;

import io.github.junhkang.springboottesting.domain.ProductDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper {
    List<ProductDTO> findAll();
    ProductDTO findById(Long id);
    void insert(ProductDTO product);
    void update(ProductDTO product);
    void delete(Long id);
}