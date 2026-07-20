package com.jipdaum_spring.domain.jipdaumuser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JipdaumUserRepository extends JpaRepository<JipdaumUser, Long> {
    Optional<JipdaumUser> findByEmail(String email);
    List<JipdaumUser> findAllByUsernameIgnoreCase(String username);
    Optional<JipdaumUser> findByUsername(String username);
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
}
