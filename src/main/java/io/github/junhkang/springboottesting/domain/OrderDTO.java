package io.github.junhkang.springboottesting.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderDTO {
    private Long id;
    private LocalDateTime orderDate;
    private Long userId;
    private String username;
    private String userEmail;
    private Long productId;
    private String productName;
    private String productDescription;
    private Double productPrice;
    private Integer productStock;
    private Integer quantity;
    private String status;
    private Double totalAmount;
}