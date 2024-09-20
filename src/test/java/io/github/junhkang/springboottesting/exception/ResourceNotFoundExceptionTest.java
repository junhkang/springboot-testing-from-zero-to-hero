package io.github.junhkang.springboottesting.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ResourceNotFoundExceptionTest {
    @Test
    @DisplayName("기본 생성자 테스트")
    void testDefaultConstructor() {
        // Given & When: 기본 생성자를 사용해 예외 생성
        ResourceNotFoundException exception = new ResourceNotFoundException();

        // Then: 예외 메시지가 null인지 확인
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    @DisplayName("메시지를 포함하는 생성자 테스트")
    void testMessageConstructor() {
        // Given: 예외 메시지
        String message = "Resource not found";

        // When: 메시지를 포함하는 생성자를 사용해 예외 생성
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then: 예외 메시지가 설정되었는지 확인
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("메시지와 원인(cause)을 포함하는 생성자 테스트")
    void testMessageAndCauseConstructor() {
        // Given: 예외 메시지와 원인 예외
        String message = "Resource not found";
        Throwable cause = new RuntimeException("Cause of the exception");

        // When: 메시지와 원인을 포함하는 생성자를 사용해 예외 생성
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

        // Then: 예외 메시지와 원인이 설정되었는지 확인
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}