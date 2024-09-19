package io.github.junhkang.springboottesting.repository.mybatis;

import io.github.junhkang.springboottesting.domain.OrderDTO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    List<OrderDTO> findAll();
    OrderDTO findById(@Param("id") Long id);
    List<OrderDTO> findByUserId(@Param("userId") Long userId);
    List<OrderDTO> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    void insert(OrderDTO order);
    void update(OrderDTO order);
    void delete(@Param("id") Long id);
}