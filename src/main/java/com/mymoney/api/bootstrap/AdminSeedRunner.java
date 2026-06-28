package com.mymoney.api.bootstrap;

import com.mymoney.api.config.AdminSeedProperties;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.member.FamilyRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeedRunner implements CommandLineRunner {

    private final FamilyMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    public AdminSeedRunner(
            FamilyMemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            AdminSeedProperties properties
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        memberRepository.findByEmailIgnoreCase(properties.email())
                .orElseGet(() -> {
                    FamilyMember member = new FamilyMember();
                    member.setName(properties.name());
                    member.setEmail(properties.email().toLowerCase());
                    member.setPasswordHash(passwordEncoder.encode(properties.password()));
                    member.setRole(FamilyRole.ADMIN);
                    member.setActive(true);
                    member.setAllowanceEnabled(false);
                    return memberRepository.save(member);
                });
    }
}
