package io.github.junhkang.springboottesting.specification;

import com.jayway.jsonpath.JsonPath;
import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.service.ProductService;
import io.github.junhkang.springboottesting.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class OrderQuantitySpecification {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService users;

    @Autowired
    private ProductService products;

    private Long userId;
    private Long productId;

    @BeforeEach
    void createFixtures() {
        User user = new User();
        user.setUsername("order-spec-user");
        user.setEmail("order-spec@example.com");
        userId = users.createUser(user).getId();

        Product product = new Product();
        product.setName("order-spec-product");
        product.setPrice(100.0);
        product.setStock(5);
        productId = products.createProduct(product).getId();
    }

    @ParameterizedTest(name = "ORD-01 주문 수량 {0}, 남은 재고 {1}")
    @CsvSource({"1, 4, 100.0", "5, 0, 500.0"})
    void createsOrderWithinStock(int quantity, int remainingStock, double totalAmount) throws Exception {
        long orderId = createPendingOrder(quantity);

        expectStoredOrder(orderId, quantity, totalAmount);
        expectStock(remainingStock);
    }

    @ParameterizedTest(name = "ORD-02 주문 생성 거부 수량 {0}")
    @ValueSource(ints = {0, -1, 6})
    void rejectsInvalidCreationWithoutChangingStock(int quantity) throws Exception {
        createOrder(quantity).andExpect(status().isBadRequest());

        mvc.perform(get("/orders/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        expectStock(5);
    }

    @ParameterizedTest(name = "ORD-03 변경 수량 {0}, 남은 재고 {1}")
    @CsvSource({"1, 4, 100.0", "5, 0, 500.0"})
    void updatesPendingOrderWithinStock(int newQuantity, int remainingStock, double totalAmount) throws Exception {
        long orderId = createPendingOrder(2);

        updateQuantity(orderId, newQuantity).andExpect(status().isOk());

        expectStoredOrder(orderId, newQuantity, totalAmount);
        expectStock(remainingStock);
    }

    @ParameterizedTest(name = "ORD-04 수량 변경 거부 수량 {0}")
    @ValueSource(ints = {0, -1, 6})
    void rejectsInvalidUpdateWithoutChangingOrderOrStock(int newQuantity) throws Exception {
        long orderId = createPendingOrder(2);

        updateQuantity(orderId, newQuantity).andExpect(status().isBadRequest());

        expectStoredOrder(orderId, 2, 200.0);
        expectStock(3);
    }

    private long createPendingOrder(int quantity) throws Exception {
        String response = createOrder(quantity)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(response).read("$.id", Long.class);
    }

    private ResultActions createOrder(int quantity) throws Exception {
        return mvc.perform(post("/orders")
                .param("userId", userId.toString())
                .param("productId", productId.toString())
                .param("quantity", Integer.toString(quantity)));
    }

    private ResultActions updateQuantity(long orderId, int newQuantity) throws Exception {
        return mvc.perform(put("/orders/{id}/quantity", orderId)
                .param("newQuantity", Integer.toString(newQuantity)));
    }

    private void expectStoredOrder(long orderId, int quantity, double totalAmount) throws Exception {
        mvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(quantity))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(totalAmount));
    }

    private void expectStock(int stock) throws Exception {
        mvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(stock));
    }
}
