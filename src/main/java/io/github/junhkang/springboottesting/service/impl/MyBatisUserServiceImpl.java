package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.domain.UserDTO;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.UserMapper;
import io.github.junhkang.springboottesting.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("mybatis")
public class MyBatisUserServiceImpl implements UserService {
    private final UserMapper userMapper;

    public MyBatisUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.findAll().stream()
                .map(dto -> {
                    User user = new User();
                    user.setId(dto.getId());
                    user.setUsername(dto.getUsername());
                    user.setEmail(dto.getEmail());
                    return user;
                })
                .collect(Collectors.toList());
    }

    @Override
    public User getUserById(Long id) {
        UserDTO dto = userMapper.findById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("User not found with id " + id);
        }
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        return user;
    }

    @Override
    public User createUser(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("User name is required.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email is required.");
        }

        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        userMapper.insert(dto);
        user.setId(dto.getId());
        return user;
    }
}