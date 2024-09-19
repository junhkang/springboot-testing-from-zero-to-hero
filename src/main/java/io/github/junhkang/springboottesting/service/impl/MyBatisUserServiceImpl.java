package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.User;
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
        // 필요한 변환 로직이 있다면 추가
        return userMapper.findAll().stream()
                .map(dto -> {
                    User user = new User();
                    user.setId(dto.getId());
                    user.setUsername(dto.getUsername());
                    return user;
                })
                .collect(Collectors.toList());
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