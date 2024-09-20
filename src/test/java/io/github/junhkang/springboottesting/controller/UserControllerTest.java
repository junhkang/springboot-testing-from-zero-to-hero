package io.github.junhkang.springboottesting.controller;

import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    /**
     * 모든 사용자 조회 테스트
     */
    @Test
    @DisplayName("모든 사용자 조회 테스트")
    void testGetAllUsers() throws Exception {
        // Given: Mocking the service layer
        User user = new User();
        user.setId(1L);
        user.setUsername("test_user");
        Mockito.when(userService.getAllUsers()).thenReturn(Collections.singletonList(user));

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].username", is("test_user")));
    }

    /**
     * 사용자 ID로 조회 테스트
     */
    @Test
    @DisplayName("사용자 ID로 조회 테스트")
    void testGetUserById() throws Exception {
        // Given: Mocking the service layer
        User user = new User();
        user.setId(1L);
        user.setUsername("test_user");
        Mockito.when(userService.getUserById(anyLong())).thenReturn(user);

        // When & Then: GET 요청을 수행하고 응답을 검증
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("test_user")));
    }

    /**
     * 사용자 생성 테스트
     */
    @Test
    @DisplayName("사용자 생성 테스트")
    void testCreateUser() throws Exception {
        // Given: Mocking the service layer
        User user = new User();
        user.setId(1L);
        user.setUsername("new_user");
        Mockito.when(userService.createUser(any(User.class))).thenReturn(user);

        // When & Then: POST 요청을 수행하고 응답을 검증
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"new_user\", \"email\": \"new_user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("new_user")));
    }
}