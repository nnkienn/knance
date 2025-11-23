package com.knance;

import com.knance.common.domain.NeonTestUser;
import com.knance.common.infra.NeonTestUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@SpringBootApplication
public class KnanceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnanceBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner neonTestRunner(NeonTestUserRepository repository) {
        return args -> {
            String email = "neon-test-" + Instant.now().toEpochMilli() + "@example.com";
            NeonTestUser user = new NeonTestUser(email);
            NeonTestUser saved = repository.save(user);
            System.out.println("✅ Saved NeonTestUser with id=" + saved.getId() + ", email=" + saved.getEmail());
        };
    }
}
