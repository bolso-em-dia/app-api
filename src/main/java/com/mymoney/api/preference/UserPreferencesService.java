package com.mymoney.api.preference;

import com.mymoney.api.account.Account;
import com.mymoney.api.account.AccountRepository;
import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.auth.AuthenticatedMemberResolver;
import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.preference.api.request.UpdateUserPreferencesRequest;
import com.mymoney.api.preference.api.response.UserPreferencesResponse;
import com.mymoney.api.shared.DateProvider;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private static final String DEFAULT_LOCALE = "pt-BR";
    private static final boolean DEFAULT_SHOW_BALANCE_WITH_BUDGETS = false;
    private static final boolean DEFAULT_SHOW_FOREIGN_CURRENCY = false;
    private static final Set<String> SUPPORTED_LOCALES = Set.of("pt-BR", "en-US");

    private final MemberPreferencesRepository memberPreferencesRepository;
    private final AccountRepository accountRepository;
    private final DateProvider dateProvider;
    private final AuthenticatedMemberResolver authenticatedMemberResolver;
    private final AuditorResolver auditorResolver;

    @Transactional(readOnly = true)
    public UserPreferencesResponse getCurrentUserPreferences() {
        return resolvePreferences(authenticatedMemberResolver.resolve());
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
        var member = authenticatedMemberResolver.resolve();
        validateLocale(request.locale());

        var preferences = memberPreferencesRepository
                .findDetailedByMemberId(member.getId())
                .orElseGet(() -> {
                    var created = new MemberPreferences();
                    created.setMember(member);
                    return created;
                });

        preferences.setLocale(request.locale().trim());
        preferences.setShowBalanceWithBudgets(request.showBalanceWithBudgets());
        preferences.setShowForeignCurrency(request.showForeignCurrency());
        preferences.setDefaultAccount(resolveDefaultAccount(request.defaultAccountId()));

        var saved = memberPreferencesRepository.save(preferences);
        log.info(
                "Member preferences updated: id={}, defaultAccountId={}, locale={}, memberId={}",
                saved.getId(),
                saved.getDefaultAccount() != null ? saved.getDefaultAccount().getId() : null,
                saved.getLocale(),
                auditorResolver.resolveMemberId());
        return toResponse(saved);
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
        var normalized = locale == null ? "" : locale.trim();
        if (!SUPPORTED_LOCALES.contains(normalized)) {
            throw new CodedResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.UNSUPPORTED_LOCALE);
        }
    }

    private Account resolveDefaultAccount(UUID defaultAccountId) {
        if (defaultAccountId == null) {
            return null;
        }

        var account = accountRepository
                .findById(defaultAccountId)
                .orElseThrow(() -> new CodedResponseStatusException(HttpStatus.NOT_FOUND, ErrorCode.ACCOUNT_NOT_FOUND));

        if (!isAccountActiveInMonth(account, dateProvider.currentReferenceMonth())) {
            throw new CodedResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DEFAULT_ACCOUNT_INACTIVE);
        }

        return account;
    }

    private boolean isAccountActiveInMonth(Account account, LocalDate referenceMonth) {
        return !account.getCreatedInMonth().isAfter(referenceMonth)
                && (account.getArchivedFromMonth() == null
                        || account.getArchivedFromMonth().isAfter(referenceMonth));
    }
}
