package com.kumdoriGrow.backend.domain.receipt;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kumdoriGrow.backend.domain.user.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
