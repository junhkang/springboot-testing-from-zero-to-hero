package io.github.junhkang.springboottesting.service.impl;

import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.exception.ResourceNotFoundException;
import io.github.junhkang.springboottesting.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 테스트 클래스: JpaUserServiceImplTest
 *
 * 이 클래스는 JpaUserServiceImpl 서비스 구현체의 비즈니스 로직을 검증하기 위한 단위 테스트를 제공합니다.
 * @DataJpaTest 어노테이션을 사용하여 JPA 관련 컴포넌트만 로드하고, @ActiveProfiles("jpa")를 통해
 * 'jpa' 프로파일을 활성화하여 JPA 관련 설정과 빈만 로드합니다.
 */
@DataJpaTest
@Import(JpaUserServiceImpl.class)
@ActiveProfiles("jpa")
class JpaUserServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JpaUserServiceImpl userService;

    private User testUser;

    /**
     * 테스트 전 데이터 초기화
     *
     * @BeforeEach 어노테이션을 사용하여 각 테스트 메서드 실행 전에 실행됩니다.
     * 테스트에 필요한 사용자를 생성 및 저장합니다.
     */
    @BeforeEach
    void setUp() {
        // Given: 테스트에 사용할 사용자 생성 및 저장
        testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test.user@example.com");
        // 추가적인 필드가 있다면 설정
        userRepository.save(testUser);
    }

    /**
     * 조회 관련 테스트 그룹
     */
    @Nested
    @DisplayName("조회 관련 테스트")
    class RetrievalTests {

        /**
         * 모든 사용자 조회 테스트
         */
        @Test
        @DisplayName("모든 사용자 조회 테스트")
        void testGetAllUsers() {
            // Given: data.sql에서 미리 생성된 사용자들이 존재한다고 가정

            // When: 모든 사용자를 조회
            List<User> users = userService.getAllUsers();

            // Then: data.sql에서 미리 생성된 사용자 수 + 테스트에서 생성한 사용자 수를 검증
            // 예를 들어, data.sql에서 3개의 사용자가 미리 생성되어 있다고 가정하면 총 4개
            assertThat(users).hasSize(4); // data.sql에서 3개의 사용자 + setUp()에서 1개
        }

        /**
         * 사용자 ID로 사용자 조회 테스트 - 존재하는 ID
         */
        @Test
        @DisplayName("사용자 ID로 사용자 조회 테스트 - 존재하는 ID")
        void testGetUserByIdExists() {
            // Given: 테스트에서 생성한 사용자의 ID

            // When: 존재하는 사용자 ID로 사용자를 조회
            User foundUser = userService.getUserById(testUser.getId());

            // Then: 조회된 사용자가 존재하고, 상세 정보가 올바른지 검증
            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getId()).isEqualTo(testUser.getId());
            assertThat(foundUser.getUsername()).isEqualTo("test_user");
            assertThat(foundUser.getEmail()).isEqualTo("test.user@example.com");
        }

        /**
         * 사용자 ID로 사용자 조회 테스트 - 존재하지 않는 ID
         */
        @Test
        @DisplayName("사용자 ID로 사용자 조회 테스트 - 존재하지 않는 ID")
        void testGetUserByIdNotExists() {
            // Given: 존재하지 않는 사용자 ID
            Long nonExistentId = 999L;

            // When & Then: 사용자 조회 시 ResourceNotFoundException이 발생하는지 검증
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                userService.getUserById(nonExistentId);
            });

            assertThat(exception.getMessage()).isEqualTo("User not found with id " + nonExistentId);
        }
    }

    /**
     * 생성 관련 테스트 그룹
     */
    @Nested
    @DisplayName("생성 관련 테스트")
    class CreationTests {

        /**
         * 사용자 생성 테스트 - 성공 케이스
         */
        @Test
        @DisplayName("사용자 생성 테스트 - 성공 케이스")
        @Transactional
        void testCreateUserSuccess() {
            // Given: 새로 생성할 사용자 정보
            User newUser = new User();
            newUser.setUsername("new_user");
            newUser.setEmail("new.user@example.com");
            // 추가적인 필드가 있다면 설정

            // When: 사용자 생성
            User createdUser = userService.createUser(newUser);

            // Then: 생성된 사용자가 정상적으로 저장되었는지 검증
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getId()).isNotNull();
            assertThat(createdUser.getUsername()).isEqualTo("new_user");
            assertThat(createdUser.getEmail()).isEqualTo("new.user@example.com");

            // Then: 데이터베이스에 저장된 사용자 수가 증가했는지 검증
            List<User> users = userService.getAllUsers();
            assertThat(users).hasSize(5); // data.sql에서 3개 + setUp()에서 1개 + 이 테스트에서 1개 = 5개
        }

        /**
         * 사용자 생성 테스트 - 실패 케이스
         */
        @Nested
        @DisplayName("사용자 생성 테스트 - 실패 케이스")
        class CreateUserFailureTests {

            /**
             * 사용자 생성 테스트 - 필수 필드 누락 (사용자 이름)
             */
            @Test
            @DisplayName("사용자 생성 테스트 - 필수 필드 누락 (사용자 이름)")
            void testCreateUserWithMissingUsername() {
                // Given: 사용자 이름이 누락된 사용자 정보
                User incompleteUser = new User();
                incompleteUser.setEmail("incomplete.user@example.com");
                // 추가적인 필드가 있다면 설정

                // When & Then: 사용자 생성 시 IllegalArgumentException이 발생하는지 검증
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    userService.createUser(incompleteUser);
                });

                assertThat(exception.getMessage()).isEqualTo("Username is required.");
            }

            /**
             * 사용자 생성 테스트 - 필수 필드 누락 (이메일)
             */
            @Test
            @DisplayName("사용자 생성 테스트 - 필수 필드 누락 (이메일)")
            void testCreateUserWithMissingEmail() {
                // Given: 이메일이 누락된 사용자 정보
                User incompleteUser = new User();
                incompleteUser.setUsername("incomplete_user");
                // 추가적인 필드가 있다면 설정

                // When & Then: 사용자 생성 시 IllegalArgumentException이 발생하는지 검증
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    userService.createUser(incompleteUser);
                });

                assertThat(exception.getMessage()).isEqualTo("Email is required.");
            }

            /**
             * 사용자 생성 테스트 - 잘못된 이메일 형식
             */
            @Test
            @DisplayName("사용자 생성 테스트 - 잘못된 이메일 형식")
            void testCreateUserWithInvalidEmail() {
                // Given: 잘못된 이메일 형식의 사용자 정보
                User invalidEmailUser = new User();
                invalidEmailUser.setUsername("invalid_email_user");
                invalidEmailUser.setEmail("invalid-email"); // 잘못된 형식

                // When & Then: 사용자 생성 시 IllegalArgumentException이 발생하는지 검증
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    userService.createUser(invalidEmailUser);
                });

                assertThat(exception.getMessage()).isEqualTo("Invalid email format.");
            }
        }
    }
}