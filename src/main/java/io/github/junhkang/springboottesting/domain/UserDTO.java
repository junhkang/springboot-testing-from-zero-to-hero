package io.github.junhkang.springboottesting.domain;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
}