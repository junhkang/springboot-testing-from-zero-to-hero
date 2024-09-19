package io.github.junhkang.springboottesting.service;

import io.github.junhkang.springboottesting.domain.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(User user);
}