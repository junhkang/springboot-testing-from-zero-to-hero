package io.github.junhkang.springboottesting.controller;

import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
@DisplayName("ProductController 테스트")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("모든 상품 조회 테스트")
    void testGetAllProducts() throws Exception {
        // Given: Mocking service layer
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        Mockito.when(productService.getAllProducts()).thenReturn(Collections.singletonList(product));

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Test Product")));
    }

    @Test
    @DisplayName("상품 ID로 상품 조회 테스트")
    void testGetProductById() throws Exception {
        // Given: Mocking service layer
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        Mockito.when(productService.getProductById(anyLong())).thenReturn(product);

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Product")));
    }

    @Test
    @DisplayName("상품 생성 테스트")
    void testCreateProduct() throws Exception {
        // Given: Mocking service layer
        Product product = new Product();
        product.setId(1L);
        product.setName("New Product");
        Mockito.when(productService.createProduct(any(Product.class))).thenReturn(product);

        // When & Then: POST 요청을 수행하고 응답을 검증
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Product\", \"description\": \"New Description\", \"price\": 100.0, \"stock\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("New Product")));
    }
}