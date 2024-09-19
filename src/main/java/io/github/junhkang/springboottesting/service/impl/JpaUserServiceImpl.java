package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.repository.jpa.UserRepository;
import io.github.junhkang.springboottesting.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("jpa")
public class JpaUserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public JpaUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return null;
    }

    @Override
    public User createUser(User user) {
        return null;
    }
}