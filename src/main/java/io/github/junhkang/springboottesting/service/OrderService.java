package io.github.junhkang.springboottesting.service;

import io.github.junhkang.springboottesting.domain.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    List<Order> getAllOrders();
    Order getOrderById(Long id);
    Order createOrder(Long userId, Long productId, Integer quantity);
    Order cancelOrder(Long id);
    Order updateOrderQuantity(Long id, Integer newQuantity);
    List<Order> getOrdersByUserId(Long userId);
    List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    Double calculateTotalAmount(Long id);
}