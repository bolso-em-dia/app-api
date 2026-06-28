package com.mymoney.api.envelope;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnvelopeModelRepository extends JpaRepository<EnvelopeModel, UUID> {

    @Query(
            """
            select distinct e
            from EnvelopeModel e
            left join fetch e.categories
            left join fetch e.ownerMember
            where e.createdInMonth <= :referenceMonth
              and (e.archivedFromMonth is null or e.archivedFromMonth > :referenceMonth)
            """)
    List<EnvelopeModel> findActiveForMonth(LocalDate referenceMonth);

    @Query(
            """
            select distinct e
            from EnvelopeModel e
            left join fetch e.categories
            left join fetch e.ownerMember
            where e.id = :id
            """)
    Optional<EnvelopeModel> findWithAssociationsById(UUID id);

    boolean existsByOwnerMemberIdAndType(UUID ownerMemberId, EnvelopeType type);
}
