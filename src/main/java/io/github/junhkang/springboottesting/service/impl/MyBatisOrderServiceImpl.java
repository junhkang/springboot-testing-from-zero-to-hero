package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.*;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.OrderMapper;
import io.github.junhkang.springboottesting.repository.mybatis.ProductMapper;
import io.github.junhkang.springboottesting.repository.mybatis.UserMapper;
import io.github.junhkang.springboottesting.service.OrderService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("mybatis")
public class MyBatisOrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public MyBatisOrderServiceImpl(OrderMapper orderMapper, UserMapper userMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderMapper.findAll().stream()
                .map(dto -> mapToOrder(dto))
                .collect(Collectors.toList());
    }

    @Override
    public Order getOrderById(Long id) {
        OrderDTO dto = orderMapper.findById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Order not found with id " + id);
        }
        return mapToOrder(dto);
    }

    @Override
    @Transactional
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        UserDTO userDTO = userMapper.findById(userId);
        if (userDTO == null) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }

        ProductDTO productDTO = productMapper.findById(productId);
        if (productDTO == null) {
            throw new ResourceNotFoundException("Product not found with id " + productId);
        }

        if (productDTO.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product id " + productId);
        }

        // 재고 업데이트
        productDTO.setStock(productDTO.getStock() - quantity);
        productMapper.update(productDTO);

        // 주문 생성
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderDate(LocalDateTime.now());
        orderDTO.setUserId(userId);
        orderDTO.setProductId(productId);
        orderDTO.setQuantity(quantity);
        orderDTO.setStatus(OrderStatus.PENDING.name());
        orderDTO.setTotalAmount(productDTO.getPrice() * quantity);
        orderMapper.insert(orderDTO);

        // 결과 반환
        return mapToOrder(orderDTO);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long id) {
        OrderDTO dto = orderMapper.findById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Order not found with id " + id);
        }

        OrderStatus currentStatus = OrderStatus.valueOf(dto.getStatus());
        if (currentStatus != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be canceled.");
        }

        // 상태 업데이트
        dto.setStatus(OrderStatus.CANCELED.name());
        orderMapper.update(dto);

        // 재고 복구
        ProductDTO productDTO = productMapper.findById(dto.getProductId());
        productDTO.setStock(productDTO.getStock() + dto.getQuantity());
        productMapper.update(productDTO);

        return mapToOrder(dto);
    }

    @Override
    @Transactional
    public Order updateOrderQuantity(Long id, Integer newQuantity) {
        OrderDTO dto = orderMapper.findById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Order not found with id " + id);
        }

        OrderStatus currentStatus = OrderStatus.valueOf(dto.getStatus());
        if (currentStatus != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be updated.");
        }

        ProductDTO productDTO = productMapper.findById(dto.getProductId());
        int difference = newQuantity - dto.getQuantity();

        if (difference > 0 && productDTO.getStock() < difference) {
            throw new IllegalArgumentException("Insufficient stock to increase quantity.");
        }

        // 재고 업데이트
        productDTO.setStock(productDTO.getStock() - difference);
        productMapper.update(productDTO);

        // 주문 업데이트
        dto.setQuantity(newQuantity);
        dto.setTotalAmount(productDTO.getPrice() * newQuantity);
        orderMapper.update(dto);

        return mapToOrder(dto);
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        UserDTO userDTO = userMapper.findById(userId);
        if (userDTO == null) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }

        return orderMapper.findByUserId(userId).stream()
                .map(dto -> mapToOrder(dto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderMapper.findByOrderDateBetween(startDate, endDate).stream()
                .map(dto -> mapToOrder(dto))
                .collect(Collectors.toList());
    }

    @Override
    public Double calculateTotalAmount(Long id) {
        OrderDTO dto = orderMapper.findById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Order not found with id " + id);
        }
        return dto.getTotalAmount();
    }

    // DTO를 Order 엔티티로 변환하는 메서드
    private Order mapToOrder(OrderDTO dto) {
        Order order = new Order();
        order.setId(dto.getId());
        order.setOrderDate(dto.getOrderDate());

        // User 설정
        User user = new User();
        user.setId(dto.getUserId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getUserEmail());
        order.setUser(user);

        // Product 설정
        Product product = new Product();
        product.setId(dto.getProductId());
        product.setName(dto.getProductName());
        product.setDescription(dto.getProductDescription());
        product.setPrice(dto.getProductPrice());
        product.setStock(dto.getProductStock());
        order.setProduct(product);

        order.setQuantity(dto.getQuantity());
        order.setStatus(OrderStatus.valueOf(dto.getStatus()));
        order.setTotalAmount(dto.getTotalAmount());

        return order;
    }
}