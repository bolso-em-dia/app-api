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
        var member = new FamilyMember();
        member.setName("Test Member");
        member.setEmail("member@example.com");
        member.setPasswordHash("encoded-password");
        member.setRole(FamilyRole.USER);
        member.setActive(true);
        member.setAllowanceEnabled(false);
        member.setMustChangePassword(false);
        customizer.accept(member);
        return member;
    }
}
