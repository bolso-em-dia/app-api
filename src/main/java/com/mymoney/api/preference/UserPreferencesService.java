package com.mymoney.api.preference;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.preference.api.request.UpdateUserPreferencesRequest;
import com.mymoney.api.preference.api.response.UserPreferencesResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private static final String DEFAULT_LOCALE = "pt-BR";
    private static final boolean DEFAULT_SHOW_BALANCE_WITH_BUDGETS = false;
    private static final boolean DEFAULT_SHOW_FOREIGN_CURRENCY = false;
    private static final Set<String> SUPPORTED_LOCALES = Set.of("pt-BR", "en-US");

    private final MemberPreferencesRepository memberPreferencesRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public UserPreferencesResponse getCurrentUserPreferences() {
        return resolvePreferences(currentMember());
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse resolvePreferences(FamilyMember member) {
        return memberPreferencesRepository
                .findDetailedByMemberId(member.getId())
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional
    public UserPreferencesResponse updateCurrentUserPreferences(UpdateUserPreferencesRequest request) {
        FamilyMember member = currentMember();
        validateLocale(request.locale());

        MemberPreferences preferences = memberPreferencesRepository
                .findDetailedByMemberId(member.getId())
                .orElseGet(() -> {
                    MemberPreferences created = new MemberPreferences();
                    created.setMember(member);
                    return created;
                });

        preferences.setLocale(request.locale().trim());
        preferences.setShowBalanceWithBudgets(request.showBalanceWithBudgets());
        preferences.setShowForeignCurrency(request.showForeignCurrency());
        preferences.setDefaultAccount(resolveDefaultAccount(request.defaultAccountId()));

        return toResponse(memberPreferencesRepository.save(preferences));
    }

    private UserPreferencesResponse defaultResponse() {
        return new UserPreferencesResponse(
                null, DEFAULT_LOCALE, DEFAULT_SHOW_BALANCE_WITH_BUDGETS, DEFAULT_SHOW_FOREIGN_CURRENCY);
    }

    private UserPreferencesResponse toResponse(MemberPreferences preferences) {
        return new UserPreferencesResponse(
                preferences.getDefaultAccount() != null
                        ? preferences.getDefaultAccount().getId().toString()
                        : null,
                preferences.getLocale(),
                preferences.isShowBalanceWithBudgets(),
                preferences.isShowForeignCurrency());
    }

    private void validateLocale(String locale) {
        String normalized = locale == null ? "" : locale.trim();
        if (!SUPPORTED_LOCALES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Locale is not supported.");
        }
    }

    private Account resolveDefaultAccount(UUID defaultAccountId) {
        if (defaultAccountId == null) {
            return null;
        }

        Account account = accountRepository
                .findById(defaultAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found."));

        LocalDate currentReferenceMonth = currentReferenceMonth();
        boolean activeForCurrentMonth = !account.getCreatedInMonth().isAfter(currentReferenceMonth)
                && (account.getArchivedFromMonth() == null
                        || account.getArchivedFromMonth().isAfter(currentReferenceMonth));

        if (!activeForCurrentMonth) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Default account must be active for the current month.");
        }

        return account;
    }

    private FamilyMember currentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }

        return familyMemberRepository
                .findByEmailIgnoreCase(authentication.getName())
                .filter(FamilyMember::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User was not found."));
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }
}
