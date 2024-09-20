package io.github.junhkang.springboottesting.exception;

import io.github.junhkang.springboottesting.controller.UserController;
import io.github.junhkang.springboottesting.domain.User;
import io.github.junhkang.springboottesting.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("ResourceNotFoundException 처리 테스트")
    void testHandleResourceNotFoundException() throws Exception {
        // Given: userService.getUserById가 ResourceNotFoundException을 던지도록 설정
        when(userService.getUserById(anyLong())).thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then: 해당 URL로 GET 요청 시 404 상태 코드와 에러 메시지를 반환하는지 확인
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 테스트")
    void testHandleIllegalArgumentException() throws Exception {
        // Given: userService.createUser가 IllegalArgumentException을 던지도록 설정
        when(userService.getUserById(Mockito.any(Long.class))).thenThrow(new IllegalArgumentException("Invalid input"));

        // When & Then: 해당 URL로 POST 요청 시 400 상태 코드와 에러 메시지를 반환하는지 확인
        mockMvc.perform(get("/users/999"))  // 적절한 요청 메서드로 변경 가능
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input"));
    }

}