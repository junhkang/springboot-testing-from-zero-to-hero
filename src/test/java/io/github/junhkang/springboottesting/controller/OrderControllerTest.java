package io.github.junhkang.springboottesting.controller;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.OrderStatus;
import io.github.junhkang.springboottesting.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(OrderController.class)
@DisplayName("OrderController 테스트")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("모든 주문 조회 테스트")
    void testGetAllOrders() throws Exception {
        // Given: Mocking service layer
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        Mockito.when(orderService.getAllOrders()).thenReturn(Collections.singletonList(order));

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    @DisplayName("주문 ID로 주문 조회 테스트")
    void testGetOrderById() throws Exception {
        // Given: Mocking service layer
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        Mockito.when(orderService.getOrderById(1L)).thenReturn(order);

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("주문 생성 테스트")
    void testCreateOrder() throws Exception {
        // Given: Mocking service layer
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        Mockito.when(orderService.createOrder(anyLong(), anyLong(), any())).thenReturn(order);

        // When & Then: POST 요청을 수행하고 응답을 검증
        mockMvc.perform(post("/orders")
                        .param("userId", "1")
                        .param("productId", "1")
                        .param("quantity", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("주문 취소 테스트")
    void testCancelOrder() throws Exception {
        // Given: Mocking service layer
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CANCELED);
        Mockito.when(orderService.cancelOrder(1L)).thenReturn(order);

        // When & Then: DELETE 요청을 수행하고 응답을 검증
        mockMvc.perform(delete("/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("CANCELED")));
    }

    @Test
    @DisplayName("주문 수량 업데이트 테스트")
    void testUpdateOrderQuantity() throws Exception {
        // Given: Mocking service layer
        Order order = new Order();
        order.setId(1L);
        order.setQuantity(5);
        Mockito.when(orderService.updateOrderQuantity(anyLong(), any())).thenReturn(order);

        // When & Then: PUT 요청을 수행하고 응답을 검증
        mockMvc.perform(put("/orders/1/quantity")
                        .param("newQuantity", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.quantity", is(5)));
    }

    @Test
    @DisplayName("사용자 ID로 주문 조회 테스트")
    void testGetOrdersByUserId() throws Exception {
        // Given: Mocking service layer
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        Mockito.when(orderService.getOrdersByUserId(1L)).thenReturn(Collections.singletonList(order));

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    @DisplayName("주문 날짜 범위로 주문 조회 테스트")
    void testGetOrdersByDateRange() throws Exception {
        // Given: Mocking service layer
        Order order1 = new Order();
        order1.setId(1L);
        Order order2 = new Order();
        order2.setId(2L);

        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Mockito.when(orderService.getOrdersByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(order1, order2));

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/orders/date")
                        .param("startDate", "2023-01-01T00:00")
                        .param("endDate", "2023-12-31T23:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    @DisplayName("주문 금액 계산 테스트")
    void testCalculateTotalAmount() throws Exception {
        // Given: Mocking service layer
        Mockito.when(orderService.calculateTotalAmount(1L)).thenReturn(500.0);

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/orders/1/totalAmount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(500.0)));
    }
}
