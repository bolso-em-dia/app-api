package com.mymoney.api.account;

import com.mymoney.api.account.api.request.ArchiveAccountRequest;
import com.mymoney.api.account.api.request.CreateAccountRequest;
import com.mymoney.api.account.api.request.UpdateAccountRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public Page<Account> listAll(String search, AccountListStatus status, AccountType type, Pageable pageable) {
        return accountRepository.findByFilters(normalizeSearch(search), status.name(), type, pageable);
    }

    @Transactional(readOnly = true)
    public Account getById(UUID id) {
        return accountRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found."));
    }

    @Transactional
    public Account create(CreateAccountRequest request) {
        Account account = new Account();
        apply(
                account,
                request.name(),
                request.type(),
                request.brand(),
                request.color(),
                request.closingDay(),
                request.dueDay());
        account.setCreatedInMonth(currentReferenceMonth());
        return accountRepository.save(account);
    }

    @Transactional
    public Account update(UUID id, UpdateAccountRequest request) {
        Account account = getById(id);
        apply(
                account,
                request.name(),
                request.type(),
                request.brand(),
                request.color(),
                request.closingDay(),
                request.dueDay());
        return accountRepository.save(account);
    }

    @Transactional
    public Account archive(UUID id, ArchiveAccountRequest request) {
        Account account = getById(id);
        if (request.archivedFromMonth().isBefore(account.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Archive month cannot be before the account creation month.");
        }
        account.setArchivedFromMonth(request.archivedFromMonth());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> listOptions(LocalDate referenceMonth) {
        return accountRepository.findAvailableForMonth(referenceMonth);
    }

    private void apply(
            Account account,
            String name,
            AccountType type,
            String brand,
            String color,
            Integer closingDay,
            Integer dueDay) {
        validateTypeFields(type, closingDay, dueDay);
        account.setName(name.trim());
        account.setType(type);
        account.setColor(normalizeNullable(color));

        if (type == AccountType.CREDIT_CARD) {
            account.setBrand(normalizeNullable(brand));
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
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Credit cards require both closing day and due day.");
            }
            validateDayRange("Closing day", closingDay);
            validateDayRange("Due day", dueDay);
            return;
        }

        if (closingDay != null || dueDay != null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Only credit cards accept closing day and due day.");
        }
    }

    private void validateDayRange(String fieldName, Integer day) {
        if (day < 1 || day > 31) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, fieldName + " must be between 1 and 31.");
        }
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
