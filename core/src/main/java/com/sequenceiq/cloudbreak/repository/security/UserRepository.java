package com.sequenceiq.cloudbreak.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.domain.security.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
}
