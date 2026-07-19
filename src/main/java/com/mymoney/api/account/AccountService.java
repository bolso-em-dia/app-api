package com.mymoney.api.account;

import com.mymoney.api.account.api.request.CreateAccountRequest;
import com.mymoney.api.account.api.request.UpdateAccountRequest;
import com.mymoney.api.audit.AuditorResolver;
import com.mymoney.api.error.CodedResponseStatusException;
import com.mymoney.api.error.ErrorCode;
import com.mymoney.api.shared.DateProvider;
import com.mymoney.api.shared.DayValidator;
import com.mymoney.api.shared.EntityResolver;
import com.mymoney.api.shared.InputNormalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditorResolver auditorResolver;
    private final DateProvider dateProvider;

    @Transactional(readOnly = true)
    public Page<Account> listAll(String search, AccountListStatus status, AccountType type, Pageable pageable) {
        return accountRepository.findByFilters(InputNormalizer.normalizeSearch(search), status.name(), type, pageable);
    }

    @Transactional(readOnly = true)
    public Account getById(UUID id) {
        return resolveAccount(id);
    }

    @Transactional
    public Account create(CreateAccountRequest request) {
        var account = new Account();
        apply(
                account,
                request.name(),
                request.type(),
                request.currency(),
                request.brand(),
                request.color(),
                request.closingDay(),
                request.dueDay());
        account.setCreatedInMonth(dateProvider.currentReferenceMonth());
        var saved = accountRepository.save(account);
        log.info(
                "Account created: id={}, name={}, type={}, currency={}, memberId={}",
                saved.getId(),
                saved.getName(),
                saved.getType(),
                saved.getCurrency(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public Account update(UUID id, UpdateAccountRequest request) {
        var account = getById(id);
        apply(
                account,
                request.name(),
                request.type(),
                request.currency(),
                request.brand(),
                request.color(),
                request.closingDay(),
                request.dueDay());
        var saved = accountRepository.save(account);
        log.info(
                "Account updated: id={}, name={}, type={}, currency={}, memberId={}",
                saved.getId(),
                saved.getName(),
                saved.getType(),
                saved.getCurrency(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional
    public Account archive(UUID id) {
        var account = getById(id);
        var archivedFromMonth = dateProvider.currentReferenceMonth();
        if (archivedFromMonth.isBefore(account.getCreatedInMonth())) {
            throw new CodedResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.ARCHIVE_BEFORE_ACCOUNT_CREATION);
        }
        account.setArchivedFromMonth(archivedFromMonth);
        var saved = accountRepository.save(account);
        log.info(
                "Account archived: id={}, archivedFromMonth={}, memberId={}",
                saved.getId(),
                saved.getArchivedFromMonth(),
                auditorResolver.resolveMemberId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Account> listOptions(LocalDate referenceMonth) {
        return accountRepository.findAvailableForMonth(referenceMonth);
    }

    private void apply(
            Account account,
            String name,
            AccountType type,
            CurrencyType currency,
            String brand,
            String color,
            Integer closingDay,
            Integer dueDay) {
        validateTypeFields(type, closingDay, dueDay);
        account.setName(InputNormalizer.requireNonBlank(name, "Name"));
        account.setType(type);
        account.setCurrency(currency != null ? currency : CurrencyType.BRL);
        account.setColor(InputNormalizer.normalizeNullable(color));

        if (type == AccountType.CREDIT_CARD) {
            account.setBrand(InputNormalizer.normalizeNullable(brand));
            account.setClosingDay(closingDay.shortValue());
            account.setDueDay(dueDay.shortValue());
        } else {
            account.setBrand(null);
            account.setClosingDay(null);
            account.setDueDay(null);
        }
    }

    private void validateTypeFields(AccountType type, Integer closingDay, Integer dueDay) {
        if (type == AccountType.CREDIT_CARD) {
            if (closingDay == null || dueDay == null) {
                throw new CodedResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.CREDIT_CARD_REQUIRES_DAYS);
            }
            DayValidator.validateDayRange(closingDay, "Closing day");
            DayValidator.validateDayRange(dueDay, "Due day");
            return;
        }

        if (closingDay != null || dueDay != null) {
            throw new CodedResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.NON_CREDIT_CARD_DAYS);
        }
    }

    private Account resolveAccount(UUID id) {
        return EntityResolver.resolveOrThrow(() -> accountRepository.findById(id), ErrorCode.ACCOUNT_NOT_FOUND);
    }
}
