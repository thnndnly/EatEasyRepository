package de.eateasy.household.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household")
public class Household {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Lockerer Vorfilter fuer die Rezeptauswahl im Wochenplan.
     * Liste muss aus DietTag-Whitelist kommen — Validierung im Service.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "default_diet_tags", nullable = false)
    private String[] defaultDietTags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Household() {
    }

    public Household(String name, String[] defaultDietTags) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.defaultDietTags = defaultDietTags == null ? new String[0] : defaultDietTags.clone();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (defaultDietTags == null) {
            defaultDietTags = new String[0];
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getDefaultDietTags() {
        return defaultDietTags == null ? new String[0] : defaultDietTags.clone();
    }

    public void setDefaultDietTags(String[] defaultDietTags) {
        this.defaultDietTags = defaultDietTags == null ? new String[0] : defaultDietTags.clone();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
