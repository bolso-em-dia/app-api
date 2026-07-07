package com.mymoney.api.preference;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberPreferencesRepository extends JpaRepository<MemberPreferences, UUID> {

    @Query(
            """
            select p
            from MemberPreferences p
            left join fetch p.defaultAccount
            where p.member.id = :memberId
            """)
    Optional<MemberPreferences> findDetailedByMemberId(UUID memberId);
}
