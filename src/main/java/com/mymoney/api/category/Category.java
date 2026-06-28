package com.mymoney.api.category;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 80)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(name = "created_in_month", nullable = false)
    private LocalDate createdInMonth;

    @Column(name = "archived_from_month")
    private LocalDate archivedFromMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_category_id")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Category replacementCategory;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        var now = OffsetDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA self-references are intentionally mutable associations managed by Hibernate.")
    public Category getReplacementCategory() {
        return replacementCategory;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA self-references are intentionally mutable associations managed by Hibernate.")
    public void setReplacementCategory(Category replacementCategory) {
        this.replacementCategory = replacementCategory;
    }
}
