package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 테스트 클래스: JpaProductServiceImplTest
 *
 * 이 클래스는 JpaProductServiceImpl 서비스 구현체의 비즈니스 로직을 검증하기 위한 단위 테스트를 제공합니다.
 * @DataJpaTest 어노테이션을 사용하여 JPA 관련 컴포넌트만 로드하고, @ActiveProfiles("jpa")를 통해
 * 'jpa' 프로파일을 활성화하여 JPA 관련 설정과 빈만 로드합니다.
 */
@DataJpaTest
@Import(JpaProductServiceImpl.class)
@ActiveProfiles("jpa")
class JpaProductServiceImplTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JpaProductServiceImpl productService;

    private Product testProduct;

    /**
     * 테스트 전 데이터 초기화
     *
     * @BeforeEach 어노테이션을 사용하여 각 테스트 메서드 실행 전에 실행됩니다.
     * 테스트에 필요한 상품을 생성 및 저장합니다.
     */
    @BeforeEach
    void setUp() {
        // Given: 테스트에 사용할 상품 생성 및 저장
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(100.0);
        testProduct.setStock(50);
        productRepository.save(testProduct);
    }

    /**
     * 조회 관련 테스트 그룹
     */
    @Nested
    @DisplayName("조회 관련 테스트")
    class RetrievalTests {

        /**
         * 모든 상품 조회 테스트
         */
        @Test
        @DisplayName("모든 상품 조회 테스트")
        void testGetAllProducts() {
            // Given: data.sql에서 미리 생성된 상품들이 존재한다고 가정

            // When: 모든 상품을 조회
            List<Product> products = productService.getAllProducts();

            // Then: data.sql에서 미리 생성된 상품 수 + 테스트에서 생성한 상품 수를 검증
            // 예를 들어, data.sql에서 5개의 상품이 미리 생성되어 있다고 가정하면 총 6개
            assertThat(products).hasSize(6); // data.sql에서 5개의 상품 + setUp()에서 1개
        }

        /**
         * 상품 ID로 상품 조회 테스트 - 존재하는 ID
         */
        @Test
        @DisplayName("상품 ID로 상품 조회 테스트 - 존재하는 ID")
        void testGetProductByIdExists() {
            // Given: 테스트에서 생성한 상품의 ID

            // When: 존재하는 상품 ID로 상품을 조회
            Product foundProduct = productService.getProductById(testProduct.getId());

            // Then: 조회된 상품이 존재하고, 상세 정보가 올바른지 검증
            assertThat(foundProduct).isNotNull();
            assertThat(foundProduct.getId()).isEqualTo(testProduct.getId());
            assertThat(foundProduct.getName()).isEqualTo("Test Product");
            assertThat(foundProduct.getDescription()).isEqualTo("Test Description");
            assertThat(foundProduct.getPrice()).isEqualTo(100.0);
            assertThat(foundProduct.getStock()).isEqualTo(50);
        }

        /**
         * 상품 ID로 상품 조회 테스트 - 존재하지 않는 ID
         */
        @Test
        @DisplayName("상품 ID로 상품 조회 테스트 - 존재하지 않는 ID")
        void testGetProductByIdNotExists() {
            // Given: 존재하지 않는 상품 ID
            Long nonExistentId = 999L;

            // When & Then: 상품 조회 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                productService.getProductById(nonExistentId);
            });

            assertThat(exception.getMessage()).isEqualTo("Product not found with id " + nonExistentId);
        }
    }

    /**
     * 생성 및 수정 관련 테스트 그룹
     */
    @Nested
    @DisplayName("생성 및 수정 관련 테스트")
    class CreationAndUpdateTests {

        /**
         * 상품 생성 테스트 - 성공 케이스
         */
        @Test
        @DisplayName("상품 생성 테스트 - 성공 케이스")
        @Transactional
        void testCreateProductSuccess() {
            // Given: 새로 생성할 상품 정보
            Product newProduct = new Product();
            newProduct.setName("New Product");
            newProduct.setDescription("New Description");
            newProduct.setPrice(200.0);
            newProduct.setStock(30);

            // When: 상품 생성
            Product createdProduct = productService.createProduct(newProduct);

            // Then: 생성된 상품이 정상적으로 저장되었는지 검증
            assertThat(createdProduct).isNotNull();
            assertThat(createdProduct.getId()).isNotNull();
            assertThat(createdProduct.getName()).isEqualTo("New Product");
            assertThat(createdProduct.getDescription()).isEqualTo("New Description");
            assertThat(createdProduct.getPrice()).isEqualTo(200.0);
            assertThat(createdProduct.getStock()).isEqualTo(30);

            // Then: 데이터베이스에 저장된 상품 수가 증가했는지 검증
            List<Product> products = productService.getAllProducts();
            assertThat(products).hasSize(7); // data.sql에서 5개 + setUp()에서 1개 + 이 테스트에서 1개 = 7개
        }

        /**
         * 상품 생성 테스트 - 실패 케이스 (필수 필드 누락)
         */
        @Test
        @DisplayName("상품 생성 테스트 - 실패 케이스 (필수 필드 누락)")
        void testCreateProductWithMissingFields() {
            // Given: 이름이 누락된 상품 정보
            Product incompleteProduct = new Product();
            incompleteProduct.setDescription("Incomplete Description");
            incompleteProduct.setPrice(150.0);
            incompleteProduct.setStock(20);

            // When & Then: 상품 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                productService.createProduct(incompleteProduct);
            });

            assertThat(exception.getMessage()).isEqualTo("Product name is required.");
        }

        /**
         * 상품 생성 테스트 - 실패 케이스 (가격이 음수인 경우)
         */
        @Test
        @DisplayName("상품 생성 테스트 - 실패 케이스 (가격이 음수인 경우)")
        void testCreateProductWithNegativePrice() {
            // Given: 가격이 음수인 상품 정보
            Product invalidPriceProduct = new Product();
            invalidPriceProduct.setName("Invalid Price Product");
            invalidPriceProduct.setDescription("Invalid Price Description");
            invalidPriceProduct.setPrice(-50.0);
            invalidPriceProduct.setStock(10);

            // When & Then: 상품 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                productService.createProduct(invalidPriceProduct);
            });

            assertThat(exception.getMessage()).isEqualTo("Product price cannot be negative.");
        }

        /**
         * 상품 생성 테스트 - 실패 케이스 (재고가 음수인 경우)
         */
        @Test
        @DisplayName("상품 생성 테스트 - 실패 케이스 (재고가 음수인 경우)")
        void testCreateProductWithNegativeStock() {
            // Given: 재고가 음수인 상품 정보
            Product invalidStockProduct = new Product();
            invalidStockProduct.setName("Invalid Stock Product");
            invalidStockProduct.setDescription("Invalid Stock Description");
            invalidStockProduct.setPrice(100.0);
            invalidStockProduct.setStock(-10);

            // When & Then: 상품 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                productService.createProduct(invalidStockProduct);
            });

            assertThat(exception.getMessage()).isEqualTo("Product stock cannot be negative.");
        }
    }
}