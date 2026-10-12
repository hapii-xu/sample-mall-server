package com.samplemall.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mall_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    /** SHA-256(salt + 明文)，演示用途。 */
    @Column(nullable = false, length = 64)
    private String passwordHash;

    @Column(nullable = false, length = 16)
    private String salt;

    @Column(nullable = false, length = 32)
    private String displayName;

    @Column(length = 16)
    private String phone;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    public User(String username, String passwordHash, String salt, String displayName, String phone) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.displayName = displayName;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
