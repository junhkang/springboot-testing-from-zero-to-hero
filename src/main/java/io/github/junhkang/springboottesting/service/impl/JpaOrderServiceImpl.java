package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.OrderStatus;
import io.github.junhkang.springboottesting.domain.Product;
import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.OrderRepository;
import io.github.junhkang.springboottesting.repository.jpa.ProductRepository;
import io.github.junhkang.springboottesting.repository.jpa.UserRepository;
import io.github.junhkang.springboottesting.service.OrderService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Profile("jpa")
public class JpaOrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public JpaOrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }

    @Override
    @Transactional
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product id " + productId);
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setUser(user);
        order.setProduct(product);
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(product.getPrice() * quantity);

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long id) {
        Order order = getOrderById(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be canceled.");
        }

        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        // 재고 복구
        Product product = order.getProduct();
        product.setStock(product.getStock() + order.getQuantity());
        productRepository.save(product);

        return order;
    }

    @Override
    @Transactional
    public Order updateOrderQuantity(Long id, Integer newQuantity) {
        Order order = getOrderById(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only pending orders can be updated.");
        }

        Product product = order.getProduct();
        int difference = newQuantity - order.getQuantity();

        if (difference > 0 && product.getStock() < difference) {
            throw new IllegalArgumentException("Insufficient stock to increase quantity.");
        }

        product.setStock(product.getStock() - difference);
        productRepository.save(product);

        order.setQuantity(newQuantity);
        order.setTotalAmount(product.getPrice() * newQuantity);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        return orderRepository.findByUser(user);
    }

    @Override
    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByOrderDateBetween(startDate, endDate);
    }

    @Override
    public Double calculateTotalAmount(Long id) {
        Order order = getOrderById(id);
        return order.getTotalAmount();
    }
}