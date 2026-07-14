package com.mymoney.api.support;

import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import java.util.function.Consumer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestIdentityFixtureSupport {

    public static final String ADMIN_EMAIL = "admin@bolso-em-dia.local";
    public static final String ADMIN_PASSWORD = "admin123456";
    public static final String USER_EMAIL = "user@bolso-em-dia.local";
    public static final String USER_PASSWORD = "user123456";

    private final FamilyMemberRepository familyMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public TestIdentityFixtureSupport(FamilyMemberRepository familyMemberRepository, PasswordEncoder passwordEncoder) {
        this.familyMemberRepository = familyMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public FamilyMember ensureAdminCanUseProtectedApis() {
        var admin = familyMemberRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseThrow();
        if (!admin.isMustChangePassword()) {
            return admin;
        }
        admin.setMustChangePassword(false);
        return familyMemberRepository.save(admin);
    }

    public FamilyMember ensureRegularUser() {
        return persistFamilyMember(USER_EMAIL, USER_PASSWORD, member -> {
            member.setName("Regular User");
            member.setRole(FamilyRole.USER);
            member.setActive(true);
            member.setAllowanceEnabled(false);
            member.setMustChangePassword(false);
        });
    }

    public FamilyMember persistFamilyMember(String email, String rawPassword, Consumer<FamilyMember> customizer) {
        var member = familyMemberRepository.findByEmailIgnoreCase(email).orElseGet(FamilyMemberTestFactory::create);
        member.setEmail(email);
        member.setPasswordHash(passwordEncoder.encode(rawPassword));
        member.setRole(FamilyRole.USER);
        member.setActive(true);
        member.setAllowanceEnabled(false);
        member.setMustChangePassword(false);
        customizer.accept(member);
        return familyMemberRepository.save(member);
    }

    public FamilyMember persistAllowanceMember(String name, String email, String rawPassword) {
        return persistFamilyMember(email, rawPassword, member -> {
            member.setName(name);
            member.setAllowanceEnabled(true);
        });
    }
}
