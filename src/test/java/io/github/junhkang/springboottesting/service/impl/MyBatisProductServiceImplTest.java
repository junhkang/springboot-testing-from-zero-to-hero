package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.domain.ProductDTO;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MyBatisProductServiceImpl의 단위 테스트 클래스
 * MyBatis 기반의 ProductService 구현체를 테스트하며, 각 메서드의 동작을 검증합니다.
 */
@SpringBootTest
@Import(MyBatisProductServiceImpl.class)
@ActiveProfiles("mybatis")
@DisplayName("MyBatisProductServiceImpl Test")
class MyBatisProductServiceImplTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MyBatisProductServiceImpl productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트용 기본 상품 데이터 생성 및 저장
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(100.0);
        testProduct.setStock(50);

        ProductDTO dto = new ProductDTO();
        dto.setName(testProduct.getName());
        dto.setDescription(testProduct.getDescription());
        dto.setPrice(testProduct.getPrice());
        dto.setStock(testProduct.getStock());

        productMapper.insert(dto); // ProductMapper를 통해 데이터베이스에 저장
        testProduct.setId(dto.getId()); // 저장 후 ID 할당
    }

    @Nested
    @DisplayName("조회 관련 테스트")
    class RetrievalTests {

        @Test
        @DisplayName("모든 상품 조회 테스트")
        void testGetAllProducts() {
            // When: 모든 상품을 조회
            List<Product> products = productService.getAllProducts();

            // Then: 데이터베이스에 저장된 상품이 정상적으로 조회되는지 검증
            assertThat(products).isNotNull();
            assertThat(products.size()).isGreaterThan(0); // 최소 1개 이상 있어야 함 (테스트에서 생성한 상품 포함)
        }

        @Test
        @DisplayName("상품 ID로 상품 조회 테스트 - 존재하는 ID")
        void testGetProductByIdExists() {
            // When: 존재하는 상품 ID로 조회
            Product product = productService.getProductById(testProduct.getId());

            // Then: 조회된 상품이 정상적으로 존재하며, 값이 정확한지 검증
            assertThat(product).isNotNull();
            assertThat(product.getId()).isEqualTo(testProduct.getId());
            assertThat(product.getName()).isEqualTo(testProduct.getName());
        }

        @Test
        @DisplayName("상품 ID로 상품 조회 테스트 - 존재하지 않는 ID")
        void testGetProductByIdNotExists() {
            // Given: 존재하지 않는 상품 ID
            Long nonExistentId = 999L;

            // When & Then: 조회 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                productService.getProductById(nonExistentId);
            });

            assertThat(exception.getMessage()).isEqualTo("Product not found with id " + nonExistentId);
        }
    }

    @Nested
    @DisplayName("생성 관련 테스트")
    class CreationTests {

        @Test
        @DisplayName("상품 생성 테스트")
        void testCreateProduct() {
            // Given: 새로운 상품 생성
            Product newProduct = new Product();
            newProduct.setName("New Product");
            newProduct.setDescription("New Description");
            newProduct.setPrice(200.0);
            newProduct.setStock(30);

            // When: 상품 생성
            Product createdProduct = productService.createProduct(newProduct);

            // Then: 상품이 정상적으로 생성되고, 데이터베이스에 저장되었는지 검증
            assertThat(createdProduct).isNotNull();
            assertThat(createdProduct.getId()).isNotNull();
            assertThat(createdProduct.getName()).isEqualTo("New Product");

            // Then: 데이터베이스에서 전체 상품 조회하여 상품이 추가되었는지 검증
            List<Product> products = productService.getAllProducts();
            assertThat(products).hasSizeGreaterThan(1); // 기존 + 새로 생성한 상품
        }

        @Test
        @DisplayName("상품 생성 테스트 - 필수 필드 누락")
        void testCreateProductMissingFields() {
            // Given: 필수 필드(이름)이 누락된 상품 생성
            Product incompleteProduct = new Product();
            incompleteProduct.setDescription("Missing Name");
            incompleteProduct.setPrice(150.0);
            incompleteProduct.setStock(10);

            // When & Then: 상품 생성 시 필드 누락으로 예외 발생 검증 (Optional 검증)
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                productService.createProduct(incompleteProduct);
            });

            assertThat(exception.getMessage()).contains("Product name is required");
        }
    }
}