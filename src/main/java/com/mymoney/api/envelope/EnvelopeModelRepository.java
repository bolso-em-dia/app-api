package com.mymoney.api.envelope;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnvelopeModelRepository extends JpaRepository<EnvelopeModel, UUID> {

    @Query(
            value =
                    """
                    select e.id
                    from EnvelopeModel e
                    where e.createdInMonth <= :referenceMonth
                      and (e.archivedFromMonth is null or e.archivedFromMonth > :referenceMonth)
                    """,
            countQuery =
                    """
                    select count(e)
                    from EnvelopeModel e
                    where e.createdInMonth <= :referenceMonth
                      and (e.archivedFromMonth is null or e.archivedFromMonth > :referenceMonth)
                    """)
    Page<UUID> findActiveIdsForMonth(LocalDate referenceMonth, Pageable pageable);

    @EntityGraph(attributePaths = {"categories", "ownerMember"})
    @Query(
            """
            select distinct e
            from EnvelopeModel e
            where e.id in :ids
            """)
    List<EnvelopeModel> findAllWithAssociationsByIdIn(Collection<UUID> ids);

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
