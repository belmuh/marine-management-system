package com.marine.management.modules.auth.domain;

import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.domain.BaseAuditedEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseAuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private String ipAddress;
    private String userAgent;

    public RefreshToken() {}

    public RefreshToken(String token, User user, LocalDateTime expiryDate, String ipAddress, String userAgent) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @Override
    public Object getId() {
        return id;
    }

    public UUID getTokenId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}