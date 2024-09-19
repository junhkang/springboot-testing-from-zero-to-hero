package io.github.junhkang.springboottesting.controller;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public Order createOrder(@RequestParam Long userId, @RequestParam Long productId, @RequestParam Integer quantity) {
        return orderService.createOrder(userId, productId, quantity);
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        Order canceledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(canceledOrder);
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<Order> updateOrderQuantity(@PathVariable Long id, @RequestParam Integer newQuantity) {
        Order updatedOrder = orderService.updateOrderQuantity(id, newQuantity);
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUserId(@PathVariable Long userId) {
        return orderService.getOrdersByUserId(userId);
    }

    @GetMapping("/date")
    public List<Order> getOrdersByDateRange(@RequestParam String startDate, @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return orderService.getOrdersByDateRange(start, end);
    }

    @GetMapping("/{id}/totalAmount")
    public ResponseEntity<Double> calculateTotalAmount(@PathVariable Long id) {
        Double totalAmount = orderService.calculateTotalAmount(id);
        return ResponseEntity.ok(totalAmount);
    }
}