package com.mymoney.api.member;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    Page<FamilyMember> findAllBy(Pageable pageable);

    Optional<FamilyMember> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
