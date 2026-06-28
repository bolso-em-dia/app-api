package com.mymoney.api.member.mapper;

import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.api.response.FamilyMemberResponse;
import org.springframework.stereotype.Component;

@Component
public class FamilyMemberMapper {

    public FamilyMemberResponse toResponse(FamilyMember member) {
        return new FamilyMemberResponse(
                member.getId().toString(),
                member.getName(),
                member.getEmail(),
                member.getRole().name(),
                member.isActive(),
                member.isAllowanceEnabled(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
