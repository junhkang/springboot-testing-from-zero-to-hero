package io.github.junhkang.springboottesting.repository.jpa;

import io.github.junhkang.springboottesting.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}