package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.domain.UserDTO;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.mybatis.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(MyBatisUserServiceImpl.class)
@ActiveProfiles("mybatis")
@DisplayName("MyBatisUserServiceImpl Test")
class MyBatisUserServiceImplTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MyBatisUserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: 테스트에 사용할 사용자 생성 및 저장
        testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test.user@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(testUser.getUsername());
        userDTO.setEmail(testUser.getEmail());
        userMapper.insert(userDTO);
        testUser.setId(userDTO.getId());
    }

    @Nested
    @DisplayName("사용자 조회 테스트")
    class UserRetrievalTests {

        @Test
        @DisplayName("모든 사용자 조회 테스트")
        void testGetAllUsers() {
            // Given: data.sql에서 미리 생성된 사용자들

            // When: 모든 사용자를 조회
            List<User> users = userService.getAllUsers();

            // Then: 데이터베이스에 저장된 총 사용자 수가 예상과 일치하는지 검증
            assertThat(users).isNotEmpty();
            assertThat(users).anyMatch(user -> "test_user".equals(user.getUsername()));
        }

        @Test
        @DisplayName("사용자 ID로 사용자 조회 - 존재하는 ID")
        void testGetUserByIdExists() {
            // When: 존재하는 사용자 ID로 사용자 조회
            User foundUser = userService.getUserById(testUser.getId());

            // Then: 조회된 사용자가 정상적으로 반환되었는지 검증
            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getId()).isEqualTo(testUser.getId());
            assertThat(foundUser.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(foundUser.getEmail()).isEqualTo(testUser.getEmail());
        }

        @Test
        @DisplayName("사용자 ID로 사용자 조회 - 존재하지 않는 ID")
        void testGetUserByIdNotExists() {
            // Given: 존재하지 않는 사용자 ID
            Long nonExistentId = 999L;

            // When & Then: 조회 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                userService.getUserById(nonExistentId);
            });

            assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentId);
        }
    }

    @Nested
    @DisplayName("사용자 생성 테스트")
    class UserCreationTests {

        @Test
        @DisplayName("사용자 생성 테스트 - 성공 케이스")
        void testCreateUserSuccess() {
            // Given: 새로운 사용자 정보
            User newUser = new User();
            newUser.setUsername("new_user");
            newUser.setEmail("new.user@example.com");

            // When: 사용자를 생성
            User createdUser = userService.createUser(newUser);

            // Then: 생성된 사용자가 정상적으로 저장되었는지 검증
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getId()).isNotNull();
            assertThat(createdUser.getUsername()).isEqualTo("new_user");
            assertThat(createdUser.getEmail()).isEqualTo("new.user@example.com");
        }

        @Test
        @DisplayName("사용자 생성 테스트 - 필수 필드 누락 (이름)")
        void testCreateUserMissingName() {
            // Given: 이름이 누락된 사용자
            User incompleteUser = new User();
            incompleteUser.setEmail("missing.name@example.com");

            // When & Then: 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                userService.createUser(incompleteUser);
            });

            assertThat(exception.getMessage()).isEqualTo("User name is required.");
        }

        @Test
        @DisplayName("사용자 생성 테스트 - 필수 필드 누락 (이메일)")
        void testCreateUserMissingEmail() {
            // Given: 이메일이 누락된 사용자
            User incompleteUser = new User();
            incompleteUser.setUsername("missing_email");

            // When & Then: 생성 시 IllegalArgumentException이 발생하는지 검증
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                userService.createUser(incompleteUser);
            });

            assertThat(exception.getMessage()).isEqualTo("User email is required.");
        }
    }
}