package io.github.junhkang.springboottesting.repository.mybatis;

import io.github.junhkang.springboottesting.domain.UserDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    List<UserDTO> findAll();
    UserDTO findById(Long id);
    void insert(UserDTO user);
    void update(UserDTO user);
    void delete(Long id);
}