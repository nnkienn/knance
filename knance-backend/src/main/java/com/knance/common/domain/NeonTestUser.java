package com.knance.common.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "neon_test_users")
public class NeonTestUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    protected NeonTestUser() {
        // for JPA
    }

    public NeonTestUser(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
