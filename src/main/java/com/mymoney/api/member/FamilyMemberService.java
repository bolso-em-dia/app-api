package com.mymoney.api.member;

import com.mymoney.api.member.api.request.CreateFamilyMemberRequest;
import com.mymoney.api.member.api.request.UpdateFamilyMemberRequest;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.ErrorMessage;
import com.mymoney.api.shared.InputNormalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
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

        FamilyMember member = new FamilyMember();
        member.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        member.setEmail(normalizeEmail(request.email()));
        member.setPasswordHash(passwordEncoder.encode(request.password()));
        member.setRole(request.role());
        member.setActive(true);
        member.setAllowanceEnabled(request.allowanceEnabled());
        member.setMustChangePassword(false);
        return familyMemberRepository.save(member);
    }

    @Transactional
    public FamilyMember update(UUID id, UpdateFamilyMemberRequest request) {
        FamilyMember member = getById(id);
        assertEmailAvailable(request.email(), member.getId());

        member.setName(InputNormalizer.requireNonBlank(request.name(), "Name"));
        member.setEmail(normalizeEmail(request.email()));
        member.setRole(request.role());
        member.setAllowanceEnabled(request.allowanceEnabled());
        if (request.password() != null && !request.password().isBlank()) {
            member.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return familyMemberRepository.save(member);
    }

    @Transactional
    public FamilyMember archive(UUID id) {
        FamilyMember member = getById(id);
        member.setActive(false);
        member.setAllowanceEnabled(false);
        return familyMemberRepository.save(member);
    }

    @Transactional
    public FamilyMember restore(UUID id) {
        FamilyMember member = getById(id);
        member.setActive(true);
        return familyMemberRepository.save(member);
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
