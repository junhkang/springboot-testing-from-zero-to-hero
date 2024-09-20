package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.*;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.OrderMapper;
import io.github.junhkang.springboottesting.repository.mybatis.ProductMapper;
import io.github.junhkang.springboottesting.repository.mybatis.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 테스트 클래스: MyBatisOrderServiceImplTest
 *
 * 이 클래스는 MyBatisOrderServiceImpl 서비스 구현체의 비즈니스 로직을 검증하기 위한 단위 테스트를 제공합니다.
 *
 * 주요 특징:
 * - MyBatis는 SQL 쿼리를 직접 작성하고 매퍼를 통해 데이터베이스와 상호작용합니다.
 * - 이 테스트는 MyBatis 매퍼에서 작성된 SQL 쿼리가 의도한 대로 실행되고, 데이터베이스와의 상호작용이 올바른지 검증하는 것을 중점으로 합니다.
 *
 * 테스트 고려 사항:
 * - SQL 쿼리의 정확성: 매퍼에 작성된 SQL 쿼리가 기대한 결과를 반환하는지 확인합니다.
 * - 매퍼 파일 매핑: MyBatis XML 매퍼 파일에서 정의된 쿼리와 객체 간 매핑이 올바르게 동작하는지 검증합니다.
 * - 동적 SQL: 특정 조건에 따라 쿼리가 동적으로 변하는 경우, 해당 동적 SQL이 올바르게 생성되고 실행되는지 테스트합니다.
 * - 성능 검토: 복잡한 SQL 쿼리가 올바르게 동작하는지 및 성능 상의 문제가 없는지를 추가적으로 검토할 수 있습니다.
 *
 * @SpringBootTest 어노테이션을 사용하여 MyBatis와 관련된 모든 컴포넌트를 로드하며,
 * @Import를 통해 MyBatisOrderServiceImpl 클래스를 로드하여 이 구현체를 테스트합니다.
 * @ActiveProfiles("mybatis") 어노테이션을 통해 'mybatis' 프로파일을 활성화하여
 * MyBatis 관련 설정과 빈이 올바르게 로드되는지 검증합니다.
 */
@SpringBootTest
@Import(MyBatisOrderServiceImpl.class)
@ActiveProfiles("mybatis")
@Transactional
@DisplayName("MyBatisOrderServiceImplTest")
class MyBatisOrderServiceImplTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MyBatisOrderServiceImpl orderService;

    private UserDTO testUser;
    private ProductDTO testProduct;

    /**
     * 테스트 전 데이터 초기화
     *
     * @BeforeEach 어노테이션을 사용하여 각 테스트 메서드 실행 전에 실행됩니다.
     * 테스트에 필요한 사용자와 상품을 생성 및 저장합니다.
     */
    @BeforeEach
    void setUp() {
        // Given: 테스트에 사용할 사용자 생성 및 저장
        testUser = new UserDTO();
        testUser.setUsername("test_user");
        testUser.setEmail("test.user@example.com");
        userMapper.insert(testUser); // insert 시 ID가 설정된다고 가정

        // Given: 테스트에 사용할 상품 생성 및 저장
        testProduct = new ProductDTO();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(100.0);
        testProduct.setStock(50);
        productMapper.insert(testProduct); // insert 시 ID가 설정된다고 가정
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
            // Given: data.sql에서 미리 생성된 주문들이 존재한다고 가정
            // 예를 들어, data.sql에서 5개의 주문이 미리 생성되어 있다고 가정

            // Given: 두 개의 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order1 = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);
            Order order2 = orderService.createOrder(testUser.getId(), testProduct.getId(), 1);

            // When: 모든 주문을 조회
            List<Order> orders = orderService.getAllOrders();

            // Then: 데이터베이스에 저장된 총 주문 수가 예상과 일치하는지 검증
            // 예: data.sql에서 5개의 주문 + 이 테스트에서 2개 = 총 7개
            assertThat(orders).hasSize(7);
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
            UserDTO anotherUser = new UserDTO();
            anotherUser.setUsername("another_user");
            anotherUser.setEmail("another.user@example.com");
            userMapper.insert(anotherUser);

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
            OrderDTO savedOrder1 = orderMapper.findById(order1.getId());
            savedOrder1.setOrderDate(date1);
            orderMapper.update(savedOrder1);

            OrderDTO savedOrder2 = orderMapper.findById(order2.getId());
            savedOrder2.setOrderDate(date2);
            orderMapper.update(savedOrder2);

            // When: 특정 날짜 범위를 설정하여 주문을 조회
            LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
            List<Order> ordersInRange = orderService.getOrdersByDateRange(startDate, endDate);

            // Then: 설정한 날짜 범위 내에 있는 주문들이 정확히 조회되는지 검증
            // 예: 이 테스트에서 생성한 2개 주문이 범위 내에 있으므로 총 2개
            assertThat(ordersInRange).hasSize(2);

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
            ProductDTO updatedProduct = productMapper.findById(productId);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(45); // 50 - 5 = 45
        }

        /**
         * 주문 생성 테스트 - 실패 케이스 (존재하지 않는 사용자 ID)
         */
        @Test
        @DisplayName("주문 생성 테스트 - 실패 케이스 (존재하지 않는 사용자 ID)")
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
         * 주문 생성 테스트 - 실패 케이스 (존재하지 않는 상품 ID)
         */
        @Test
        @DisplayName("주문 생성 테스트 - 실패 케이스 (존재하지 않는 상품 ID)")
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
         * 주문 생성 테스트 - 실패 케이스 (재고 부족)
         */
        @Test
        @DisplayName("주문 생성 테스트 - 실패 케이스 (재고 부족)")
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
            ProductDTO updatedProduct = productMapper.findById(productId);
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(50); // 재고 변동 없음
        }

        /**
         * 주문 수량 업데이트 테스트 - 성공 케이스 (증가)
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 성공 케이스 (증가)")
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
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
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
            OrderDTO updatedOrder = orderMapper.findById(order.getId());
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getQuantity()).isEqualTo(2);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
        }

        /**
         * 주문 수량 업데이트 테스트 - 실패 케이스 (주문 상태가 PENDING이 아님)
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 실패 케이스 (주문 상태가 PENDING이 아님)")
        void testUpdateOrderQuantityNotPending() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // Given: 주문 상태를 COMPLETED로 변경
            OrderDTO dto = orderMapper.findById(order.getId());
            dto.setStatus(OrderStatus.COMPLETED.name());
            orderMapper.update(dto);

            // When & Then: 주문 수량을 업데이트 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.updateOrderQuantity(order.getId(), 4);
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be updated.");

            // Then: 주문 수량이 변경되지 않았는지 검증
            OrderDTO updatedOrder = orderMapper.findById(order.getId());
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getQuantity()).isEqualTo(2);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
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
        void testCancelOrderSuccess() {
            // Given: 주문을 생성 및 저장 (서비스 메서드 사용)
            Order order = orderService.createOrder(testUser.getId(), testProduct.getId(), 2);

            // When: 주문을 취소
            Order canceledOrder = orderService.cancelOrder(order.getId());

            // Then: 주문 상태가 CANCELED로 변경되었는지 검증
            assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);

            // Then: 상품의 재고가 복구되었는지 검증
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
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
            OrderDTO dto = orderMapper.findById(order.getId());
            dto.setStatus(OrderStatus.COMPLETED.name());
            orderMapper.update(dto);

            // When & Then: 주문을 취소 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.cancelOrder(order.getId());
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be canceled.");

            // Then: 주문 상태가 변경되지 않았는지 검증
            OrderDTO updatedOrder = orderMapper.findById(order.getId());
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED.toString());

            // Then: 상품의 재고가 변경되지 않았는지 검증
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
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
            ProductDTO updatedProduct = productMapper.findById(productId);
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
            OrderDTO dto = orderMapper.findById(order.getId());
            dto.setStatus(OrderStatus.COMPLETED.name());
            orderMapper.update(dto);

            // When & Then: 주문을 취소 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.cancelOrder(order.getId());
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be canceled.");

            // Then: 주문 상태가 변경되지 않았는지 검증
            OrderDTO updatedOrder = orderMapper.findById(order.getId());
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED.toString());

            // Then: 상품의 재고가 변경되지 않았는지 검증
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
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
            OrderDTO dto = orderMapper.findById(order.getId());
            dto.setStatus(OrderStatus.COMPLETED.name());
            orderMapper.update(dto);

            // When & Then: 주문 수량을 업데이트 시도 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderService.updateOrderQuantity(order.getId(), 4);
            });

            assertThat(exception.getMessage()).isEqualTo("Only pending orders can be updated.");

            // Then: 주문 수량이 변경되지 않았는지 검증
            OrderDTO updatedOrder = orderMapper.findById(order.getId());
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getQuantity()).isEqualTo(2);
            assertThat(updatedOrder.getTotalAmount()).isEqualTo(200.0);

            // Then: 상품의 재고가 변경되지 않았는지 검증
            ProductDTO updatedProduct = productMapper.findById(testProduct.getId());
            assertThat(updatedProduct).isNotNull();
            assertThat(updatedProduct.getStock()).isEqualTo(48); // 50 - 2 = 48
        }
        /**
         * 주문 수량 업데이트 테스트 - 존재하지 않는 주문 ID
         */
        @Test
        @DisplayName("주문 수량 업데이트 테스트 - 존재하지 않는 주문 ID")
        void testUpdateOrderQuantityNotExists() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 999L;

            // When & Then: 주문 수량 업데이트 시 ResourceNotFoundException 발생 여부 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.updateOrderQuantity(nonExistentOrderId, 5);
            });

            assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentOrderId);
        }
        /**
         * 주문 취소 테스트 - 존재하지 않는 주문 ID
         */
        @Test
        @DisplayName("주문 취소 테스트 - 존재하지 않는 주문 ID")
        void testCancelOrderNotExists() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 999L;

            // When & Then: 주문 취소 시 ResourceNotFoundException 발생 여부 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.cancelOrder(nonExistentOrderId);
            });

            assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentOrderId);
        }
        /**
         * 사용자 ID로 주문 조회 테스트 - 존재하지 않는 사용자 ID
         */
        @Test
        @DisplayName("사용자 ID로 주문 조회 테스트 - 존재하지 않는 사용자 ID")
        void testGetOrdersByUserIdNotExists() {
            // Given: 존재하지 않는 사용자 ID
            Long nonExistentUserId = 999L;

            // When & Then: 주문 조회 시 ResourceNotFoundException 발생 여부 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.getOrdersByUserId(nonExistentUserId);
            });

            assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentUserId);
        }

        /**
         * 주문 금액 계산 테스트 - 존재하지 않는 주문 ID
         */
        @Test
        @DisplayName("주문 금액 계산 테스트 - 존재하지 않는 주문 ID")
        void testCalculateTotalAmountNotExists() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 999L;

            // When & Then: 총 금액 계산 시 ResourceNotFoundException 발생 여부 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                orderService.calculateTotalAmount(nonExistentOrderId);
            });

            assertThat(exception.getMessage()).isEqualTo("Order not found with id " + nonExistentOrderId);
        }
    }

}