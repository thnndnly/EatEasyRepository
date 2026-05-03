package de.eateasy.household.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household_invitation")
public class HouseholdInvitation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HouseholdInvitation() {
    }

    public HouseholdInvitation(UUID householdId, String email, String token, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.email = email;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void markAccepted(Instant when) {
        this.acceptedAt = when;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
