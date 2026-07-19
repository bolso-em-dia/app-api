package com.mymoney.api.auth;

import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedMemberResolver {

    private final FamilyMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public FamilyMember resolve() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new CodedResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_AUTHENTICATED);
        }

        return memberRepository
                .findByEmailIgnoreCase(authentication.getName())
                .filter(FamilyMember::isActive)
                .orElseThrow(
                        () -> new CodedResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorCode.ACCOUNT_DEACTIVATED));
    }
}
