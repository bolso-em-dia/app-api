package com.mymoney.api.preference;

import com.mymoney.api.account.Account;
import com.mymoney.api.member.FamilyMember;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member_preferences")
@Getter
@Setter
@NoArgsConstructor
public class MemberPreferences {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private FamilyMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_account_id")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Account defaultAccount;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(name = "show_balance_with_budgets", nullable = false)
    private boolean showBalanceWithBudgets;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime updatedAt;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA entity association intentionally returns a managed entity reference.")
    public FamilyMember getMember() {
        return member;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity association intentionally stores a managed entity reference.")
    public void setMember(FamilyMember member) {
        this.member = member;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA entity association intentionally returns a managed entity reference.")
    public Account getDefaultAccount() {
        return defaultAccount;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity association intentionally stores a managed entity reference.")
    public void setDefaultAccount(Account defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

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
}
