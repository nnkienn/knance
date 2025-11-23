package com.knance.common.infra;

import com.knance.common.domain.NeonTestUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NeonTestUserRepository extends JpaRepository<NeonTestUser, Long> {
}
