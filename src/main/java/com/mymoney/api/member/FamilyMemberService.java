package com.mymoney.api.member;

import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.member.api.request.CreateFamilyMemberRequest;
import com.mymoney.api.member.api.request.UpdateFamilyMemberRequest;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.ErrorMessage;
import com.mymoney.api.shared.InputNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final AuditorResolver auditorResolver;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<FamilyMember> listAll(String search, FamilyMemberListStatus status, Pageable pageable) {
        return familyMemberRepository.findByFilters(InputNormalizer.normalizeSearch(search), status.name(), pageable);
    }

    @Transactional(readOnly = true)
    public FamilyMember getById(UUID id) {
        return EntityResolver.resolveOrThrow(
                () -> familyMemberRepository.findById(id), ErrorMessage.FAMILY_MEMBER_NOT_FOUND.message());
    }

    @Transactional
    public FamilyMember create(CreateFamilyMemberRequest request) {
        assertEmailAvailable(request.email(), null);

        var member = new FamilyMember();
        member.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        member.setEmail(normalizeEmail(request.email()));
        member.setPasswordHash(passwordEncoder.encode(request.password()));
        member.setRole(request.role());
        member.setActive(true);
        member.setMustChangePassword(false);
        var saved = familyMemberRepository.save(member);
        log.info(
                "Family member created: id={}, email={}, role={}, memberId={}",
                saved.getId(),
                saved.getEmail(),
                saved.getRole(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public FamilyMember update(UUID id, UpdateFamilyMemberRequest request) {
        var member = getById(id);
        assertEmailAvailable(request.email(), member.getId());

        member.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        member.setEmail(normalizeEmail(request.email()));
        member.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            member.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        var saved = familyMemberRepository.save(member);
        log.info(
                "Family member updated: id={}, email={}, role={}, memberId={}",
                saved.getId(),
                saved.getEmail(),
                saved.getRole(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public FamilyMember archive(UUID id) {
        var member = getById(id);
        member.setActive(false);
        var saved = familyMemberRepository.save(member);
        log.info(
                "Family member archived: id={}, active={}, memberId={}",
                saved.getId(),
                saved.isActive(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public FamilyMember restore(UUID id) {
        var member = getById(id);
        member.setActive(true);
        var saved = familyMemberRepository.save(member);
        log.info(
                "Family member restored: id={}, active={}, memberId={}",
                saved.getId(),
                saved.isActive(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    private void assertEmailAvailable(String rawEmail, UUID currentId) {
        String normalizedEmail = normalizeEmail(rawEmail);
        familyMemberRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorMessage.EMAIL_ALREADY_IN_USE.message());
                });
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
