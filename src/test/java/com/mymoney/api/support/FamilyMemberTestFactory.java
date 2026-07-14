package com.mymoney.api.support;

import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyRole;
import java.util.function.Consumer;

public final class FamilyMemberTestFactory {

    private FamilyMemberTestFactory() {}

    public static FamilyMember create() {
        return create(member -> {});
    }

    public static FamilyMember create(Consumer<FamilyMember> customizer) {
        var member = FamilyMember.builder()
                .name("Test Member")
                .email("member@example.com")
                .passwordHash("encoded-password")
                .role(FamilyRole.USER)
                .active(true)
                .allowanceEnabled(false)
                .mustChangePassword(false)
                .build();
        customizer.accept(member);
        return member;
    }
}
