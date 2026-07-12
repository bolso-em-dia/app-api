package com.mymoney.api.member;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    @Query(
            """
            select m
            from FamilyMember m
            where (
                :search = ''
                or f_unaccent_lower(m.name) like concat('%', f_unaccent_lower(:search), '%')
                or f_unaccent_lower(m.email) like concat('%', f_unaccent_lower(:search), '%')
              )
              and (
                :status = 'ALL'
                or (:status = 'ACTIVE' and m.active = true)
                or (:status = 'ARCHIVED' and m.active = false)
              )
            """)
    Page<FamilyMember> findByFilters(String search, String status, Pageable pageable);

    Optional<FamilyMember> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
