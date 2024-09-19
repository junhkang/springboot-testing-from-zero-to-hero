package io.github.junhkang.springboottesting.repository.jpa;

import io.github.junhkang.springboottesting.domain.Order;
import io.github.junhkang.springboottesting.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}