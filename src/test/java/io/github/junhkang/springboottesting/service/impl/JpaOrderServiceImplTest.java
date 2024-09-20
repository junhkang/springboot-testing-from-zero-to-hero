package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.OrderStatus;
import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.OrderRepository;
import io.github.junhkang.springboottesting.repository.jpa.ProductRepository;
import io.github.junhkang.springboottesting.repository.jpa.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 테스트 클래스: JpaOrderServiceImplTest
 *
 * 이 클래스는 JpaOrderServiceImpl 서비스 구현체의 비즈니스 로직을 검증하기 위한 단위 테스트를 제공합니다.
 * @DataJpaTest 어노테이션을 사용하여 JPA 관련 컴포넌트만 로드하고, @ActiveProfiles("jpa")를 통해
 * 'jpa' 프로파일을 활성화하여 JPA 관련 설정과 빈만 로드합니다.
 */
@DataJpaTest
@Import(JpaOrderServiceImpl.class)
@ActiveProfiles("jpa")
class JpaOrderServiceImplTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JpaOrderServiceImpl orderService;

    private User testUser;
    private Product testProduct;

    /**
     * 테스트 전 데이터 초기화
     *
     * @BeforeEach 어노테이션을 사용하여 각 테스트 메서드 실행 전에 실행됩니다.
     * 테스트에 필요한 사용자와 상품을 생성 및 저장합니다.
     */
    @BeforeEach
    void setUp() {
        // Given: 테스트에 사용할 사용자 생성 및 저장
        testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test.user@example.com");
        userRepository.save(testUser);

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
         * 모든 주문 조회 테스트
         */
        @Test
        @DisplayName("모든 주문 조회 테스트")
        void testGetAllOrders() {
            // Given: 두 개의 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
            Order order2 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

            // When: 모든 주문을 조회
            List<Order> orders = orderService.getAllOrders();

            // Then: 데이터베이스에 저장된 주문 수를 검증 (data.sql에서 미리 생성된 주문 수를 고려)
            // 예: data.sql에서 5개의 주문이 미리 생성되어 있다고 가정하면 총 7개
            assertThat(orders).hasSize(7); // data.sql에서 5개의 주문 + 이 테스트에서 2개
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 5, 10})
        @DisplayName("다양한 수량으로 주문 생성 테스트")
        void testCreateOrderWithDifferentQuantities(int quantity) {
            Long userId = testUser.getId();
            Long productId = testProduct.getId();

            Order order = orderService.createOrder(userId, productId, quantity);

            assertThat(order.getQuantity()).isEqualTo(quantity);
        }
        /**
         * 주문 ID로 주문 조회 테스트 - 존재하는 ID
         */
        @Test
        @DisplayName("주문 ID로 주문 조회 테스트 - 존재하는 ID")
        void testGetOrderByIdExists() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 3);

            // When: 존재하는 주문 ID로 주문을 조회
            Order foundOrder = orderService.getOrderById(order.getId());

            // Then: 주문이 정상적으로 조회되고, 세부 사항이 올바른지 검증
            assertThat(foundOrder).isNotNull();
            assertThat(foundOrder.getId()).isEqualTo(order.getId());
            assertThat(foundOrder.getUser().getUsername()).isEqualTo("test_user");
            assertThat(foundOrder.getProduct().getName()).isEqualTo("Test Product");
            assertThat(foundOrder.getQuantity()).isEqualTo(3);
            assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(foundOrder.getTotalAmount()).isEqualTo(300.0);
        }

        /**
         * 주문 ID로 주문 조회 테스트 - 존재하지 않는 ID
         */
        @Test
        @DisplayName("주문 ID로 주문 조회 테스트 - 존재하지 않는 ID")
        void testGetOrderByIdNotExists() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentId = 999L;

            // When & Then: 주문 조회 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.getOrderById(nonExistentId);
            });

            assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentId);
        }

        /**
         * 사용자 ID로 주문 조회 테스트
         */
        @Test
        @DisplayName("사용자 ID로 주문 조회 테스트")
        void testGetOrdersByUserId() {
            // Given: 다른 사용자 생성 및 저장
            User anotherUser = new User();
            anotherUser.setUsername("another_user");
            anotherUser.setEmail("another.user@example.com");
            userRepository.save(anotherUser);

            // Given: testUser의 주문 생성 및 저장 (서비스 메서드 사용)
            Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

            // Given: anotherUser의 주문 생성 및 저장 (서비스 메서드 사용)
            Order order2 = orderService.createOrder(anotherUser.getId(), testProduct.getId(), 3);

            // When: testUser의 모든 주문을 조회
            List<Order> userOrders = orderService.getOrdersByUserId(testUser.getId());

            // Then: testUser의 주문만 조회되었는지 검증
            assertThat(userOrders).hasSize(1);
            assertThat(userOrders.get(0).getUser().getUsername()).isEqualTo("test_user");
        }

        /**
         * 주문 날짜 범위로 주문 조회 테스트
         */
        @Test
        @DisplayName("주문 날짜 범위로 주문 조회 테스트")
        void testGetOrdersByDateRange() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
            Order order2 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

            // 주문의 orderDate를 특정 날짜로 설정 (테스트 목적)
            LocalDateTime date1 = LocalDateTime.of(2023, 1, 1, 10, 0);
            LocalDateTime date2 = LocalDateTime.of(2023, 6, 15, 15, 30);
            Order savedOrder1 = orderRepository.findById(order1.getId()).orElseThrow();
            savedOrder1.setOrderDate(date1);
            orderRepository.save(savedOrder1);

            Order savedOrder2 = orderRepository.findById(order2.getId()).orElseThrow();
            savedOrder2.setOrderDate(date2);
            orderRepository.save(savedOrder2);

            // Given: data.sql에서 미리 생성된 5개의 주문 중 일부는 특정 날짜 범위에 속하도록 설정
            // (data.sql의 주문들이 이미 특정 날짜를 가지고 있다고 가정)

            // When: 특정 날짜 범위를 설정하여 주문을 조회
            LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
            List<Order> ordersInRange = orderService.getOrdersByDateRange(startDate, endDate);

            // Then: 설정한 날짜 범위 내에 있는 주문들이 정확히 조회되는지 검증
            assertThat(ordersInRange).hasSize(2); // 테스트에서 2개

            // Then: 조회된 주문들의 orderDate가 설정한 범위 내에 있는지 검증
            assertThat(ordersInRange).allMatch(order ->
                    !order.getOrderDate().isBefore(startDate) && !order.getOrderDate().isAfter(endDate)
            );

            // 추가 검증: 특정 주문이 포함되어 있는지 확인
            assertThat(ordersInRange)
                    .extracting(Order::getId)
                    .contains(savedOrder1.getId(), savedOrder2.getId());
        }
    }

    /**
     * 생성 및 수정 관련 테스트 그룹
     */
    @Nested
    @DisplayName("생성 및 수정 관련 테스트")
    class CreationAndUpdateTests {

        /**
         * 주문 생성 테스트 - 성공 케이스
         */
        @Test
        @DisplayName("주문 생성 테스트 - 성공 케이스")
        @Transactional
        @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
        void testCreateOrderSuccess() {
            // Given: 유효한 사용자 ID, 상품 ID, 및 수량
            Long userId = testUser.getId();
            Long productId = testProduct.getId();
            Integer quantity = 5;

            // When: 주문 생성
            Order createdOrder = orderService.createOrder(userId, productId, quantity);

            // Then: 주문이 정상적으로 생성되고, 관련 데이터가 올바르게 업데이트되었는지 검증
            assertThat(createdOrder).isNotNull();
            assertThat(createdOrder.getId()).isNotNull();
            assertThat(createdOrder.getUser().getId()).isEqualTo(userId);
            assertThat(createdOrder.getProduct().getId()).isEqualTo(productId);
            assertThat(createdOrder.getQuantity()).isEqualTo(quantity);
            assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(createdOrder.getTotalAmount()).isEqualTo(testProduct.getPrice() * quantity);

            // Then: 상품의 재고가 감소했는지 검증
            Product updatedProduct = productRepository.findById(productId).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(45); // 50 - 5 = 45
        }
        @ParameterizedTest
        @CsvSource({
                "test_user, test.user@example.com",
                "john_doe, john.doe@example.com"
        })
        @DisplayName("다양한 사용자 이름 및 이메일로 주문 생성 테스트")
        void testCreateOrderWithDifferentUsers(String username, String email) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            userRepository.save(user);

            Long productId = testProduct.getId();
            Order order = orderService.createOrder(user.getId(), productId, 1);

            assertThat(order.getUser().getUsername()).isEqualTo(username);
            assertThat(order.getUser().getEmail()).isEqualTo(email);
        }
        /**
         * 주문 생성 테스트 - 실패 케이스 (재고 부족)
         */
        @Test
        @DisplayName("주문 생성 테스트 - 실패 케이스 (재고 부족)")
        void testCreateOrderInsufficientStock() {
            // Given: 유효한 사용자 ID, 상품 ID, 및 재고보다 많은 수량
            Long userId = testUser.getId();
            Long productId = testProduct.getId();
            Integer quantity = 100; // 재고 50보다 큼

            // When & Then: 주문 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.createOrder(userId, productId, quantity);
            });

            assertThat(exception.getMessage()).isEqualTo("Insufficient stock for product id " + productId);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            Product updatedProduct = productRepository.findById(productId).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(50); // 재고 변동 없음
        }

        /**
         * 주문 수량 업데이트 테스트 - 성공 케이스 (증가)
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 성공 케이스 (증가)")
        @Transactional
        void testUpdateOrderQuantityIncrease() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // When: 주문 수량을 4로 업데이트 (증가)
            Integer newQuantity = 4;
            Order updatedOrder = orderService.updateOrderQuantity(order.getId(), newQuantity);

            // Then: 주문 수량과 총 금액이 올바르게 업데이트되었는지 검증
            assertThat(updatedOrder.getQuantity()).isEqualTo(newQuantity);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(testProduct.getPrice() * newQuantity);

            // Then: 상품의 재고가 올바르게 감소했는지 검증
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(46); // 50 - 4 = 46
        }

        /**
         * 주문 수량 업데이트 테스트 - 실패 케이스 (재고 부족)
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 실패 케이스 (재고 부족)")
        void testUpdateOrderQuantityInsufficientStock() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // When & Then: 주문 수량을 재고를 초과하는 값으로 업데이트 시도 시 IllegalArgumentException이 발생하는지 검증
            Integer newQuantity = 100; // 재고 50 - 2 + 100 = 148 > 50
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.updateOrderQuantity(order.getId(), newQuantity);
            });

            assertThat(exception.getMessage()).isEqualTo("Insufficient stock to increase quantity.");

            // Then: 주문 수량이 변경되지 않았는지 검증
            Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getQuantity()).isEqualTo(2);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
        }
        /**
         * 반복된 주문 생성 테스트 - 여러 번 주문 생성하여 성능 확인
         */
        @RepeatedTest(value = 5, name = "주문 생성 반복 테스트 {currentRepetition}/{totalRepetitions}")
        @DisplayName("주문 생성 반복 테스트")
        @Transactional
        void testCreateOrderRepeated() {
            Long userId = testUser.getId();
            Long productId = testProduct.getId();
            Integer quantity = 3;

            Order createdOrder = orderService.createOrder(userId, productId, quantity);

            assertThat(createdOrder).isNotNull();
            assertThat(createdOrder.getQuantity()).isEqualTo(quantity);
            assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    /**
     * 취소 관련 테스트 그룹
     */
    @Nested
    @DisplayName("취소 관련 테스트")
    class CancellationTests {

        /**
         * 주문 취소 테스트 - 성공 케이스
         */
        @Test
        @DisplayName("주문 취소 테스트 - 성공 케이스")
        @Transactional
        void testCancelOrderSuccess() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // When: 주문을 취소
            Order canceledOrder = orderService.cancelOrder(order.getId());

            // Then: 주문 상태가 CANCELED로 변경되었는지 검증
            assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);

            // Then: 상품의 재고가 복구되었는지 검증
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(50); // 50 - 2 + 2 = 50
        }

        /**
         * 주문 취소 테스트 - 실패 케이스 (주문 상태가 PENDING이 아님)
         */
        @Test
        @DisplayName("주문 취소 테스트 - 실패 케이스 (주문 상태가 PENDING이 아님)")
        void testCancelOrderNotPending() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // Given: 주문 상태를 COMPLETED로 변경
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            // When & Then: 주문을 취소 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.cancelOrder(order.getId());
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be canceled.");

            // Then: 주문 상태가 변경되지 않았는지 검증
            Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
        }
    }

    /**
     * 주문 금액 계산 관련 테스트 그룹
     */
    @Nested
    @DisplayName("주문 금액 계산 관련 테스트")
    class CalculateTotalAmountTests {

        /**
         * 주문 금액 계산 테스트
         */
        @Test
        @DisplayName("주문 금액 계산 테스트")
        void testCalculateTotalAmount() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 5);

            // When: 주문의 총 금액을 계산
            Double totalAmount = orderService.calculateTotalAmount(order.getId());

            // Then: 계산된 총 금액이 올바른지 검증
            assertThat(totalAmount).isEqualTo(500.0);
        }
    }

    /**
     * 예외 상황 관련 테스트 그룹
     */
    @Nested
    @DisplayName("예외 상황 관련 테스트")
    class ExceptionTests {

        /**
         * 주문 생성 테스트 - 존재하지 않는 사용자 ID
         */
        @Test
        @DisplayName("주문 생성 테스트 - 존재하지 않는 사용자 ID")
        void testCreateOrderWithNonExistentUser() {
            // Given: 존재하지 않는 사용자 ID, 유효한 상품 ID, 및 수량
            Long nonExistentUserId = 999L;
            Long productId = testProduct.getId();
            Integer quantity = 1;

            // When & Then: 주문 생성 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.createOrder(nonExistentUserId, productId, quantity);
            });

            assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentUserId);
        }

        /**
         * 주문 생성 테스트 - 존재하지 않는 상품 ID
         */
        @Test
        @DisplayName("주문 생성 테스트 - 존재하지 않는 상품 ID")
        void testCreateOrderWithNonExistentProduct() {
            // Given: 유효한 사용자 ID, 존재하지 않는 상품 ID, 및 수량
            Long userId = testUser.getId();
            Long nonExistentProductId = 999L;
            Integer quantity = 1;

            // When & Then: 주문 생성 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.createOrder(userId, nonExistentProductId, quantity);
            });

            assertThat(exception.getMessage()).isEqualTo("Product not found with id " + nonExistentProductId);
        }

        /**
         * 주문 생성 테스트 - 재고 부족
         */
        @Test
        @DisplayName("주문 생성 테스트 - 재고 부족")
        void testCreateOrderWithInsufficientStock() {
            // Given: 유효한 사용자 ID, 상품 ID, 및 재고보다 많은 수량
            Long userId = testUser.getId();
            Long productId = testProduct.getId();
            Integer quantity = 100; // 재고 50보다 큼

            // When & Then: 주문 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.createOrder(userId, productId, quantity);
            });

            assertThat(exception.getMessage()).isEqualTo("Insufficient stock for product id " + productId);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            Product updatedProduct = productRepository.findById(productId).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(50); // 재고 변동 없음
        }

        /**
         * 주문 취소 테스트 - 주문 상태가 PENDING이 아님
         */
        @Test
        @DisplayName("주문 취소 테스트 - 주문 상태가 PENDING이 아님")
        void testCancelOrderNotPending() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // Given: 주문 상태를 COMPLETED로 변경
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            // When & Then: 주문을 취소 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.cancelOrder(order.getId());
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be canceled.");

            // Then: 주문 상태가 변경되지 않았는지 검증
            Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        /**
         * 주문 수량 업데이트 테스트 - 주문 상태가 PENDING이 아님
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 주문 상태가 PENDING이 아님")
        void testUpdateOrderQuantityNotPending() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // Given: 주문 상태를 COMPLETED로 변경
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            // When & Then: 주문 수량을 업데이트 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.updateOrderQuantity(order.getId(), 4);
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be updated.");

            // Then: 주문 수량이 변경되지 않았는지 검증
            Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getQuantity()).isEqualTo(2);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);
        }

        /**
         * 주문 수량 업데이트 테스트 - 재고 부족
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 재고 부족")
        void testUpdateOrderQuantityInsufficientStock() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // When & Then: 주문 수량을 재고를 초과하는 값으로 업데이트 시도 시 IllegalArgumentException이 발생하는지 검증
            Integer newQuantity = 100; // 재고 50 - 2 + 100 = 148 > 50
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.updateOrderQuantity(order.getId(), newQuantity);
            });

            assertThat(exception.getMessage()).isEqualTo("Insufficient stock to increase quantity.");

            // Then: 주문 수량이 변경되지 않았는지 검증
            Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getQuantity()).isEqualTo(2);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElse(null);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
        }
    }
}