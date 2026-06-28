package com.mymoney.api.member;

import com.mymoney.api.member.api.request.CreateFamilyMemberRequest;
import com.mymoney.api.member.api.request.UpdateFamilyMemberRequest;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository, PasswordEncoder passwordEncoder) {
        this.familyMemberRepository = familyMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<FamilyMember> listAll() {
        return familyMemberRepository.findAll().stream()
                .sorted(Comparator.comparing(FamilyMember::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilyMember getById(UUID id) {
        return familyMemberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family member was not found."));
    }

    @Transactional
    public FamilyMember create(CreateFamilyMemberRequest request) {
        assertEmailAvailable(request.email(), null);

        FamilyMember member = new FamilyMember();
        member.setName(request.name().trim());
        member.setEmail(normalizeEmail(request.email()));
        member.setPasswordHash(passwordEncoder.encode(request.password()));
        member.setRole(request.role());
        member.setActive(true);
        member.setAllowanceEnabled(request.allowanceEnabled());
        return familyMemberRepository.save(member);
    }

    @Transactional
    public FamilyMember update(UUID id, UpdateFamilyMemberRequest request) {
        FamilyMember member = getById(id);
        assertEmailAvailable(request.email(), member.getId());

        member.setName(request.name().trim());
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
        familyMemberRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use.");
                });
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
