package com.mymoney.api.envelope;

import com.mymoney.api.category.Category;
import com.mymoney.api.category.CategoryService;
import com.mymoney.api.envelope.api.request.ArchiveEnvelopeRequest;
import com.mymoney.api.envelope.api.request.CreateEnvelopeRequest;
import com.mymoney.api.envelope.api.request.UpdateEnvelopeRequest;
import com.mymoney.api.member.FamilyMember;
import com.mymoney.api.member.FamilyMemberRepository;
import com.mymoney.api.transaction.OwnershipType;
import com.mymoney.api.transaction.Transaction;
import com.mymoney.api.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EnvelopeService {

    private final EnvelopeModelRepository envelopeModelRepository;
    private final CategoryService categoryService;
    private final FamilyMemberRepository familyMemberRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<EnvelopeView> listForMonth(LocalDate referenceMonth) {
        return envelopeModelRepository.findActiveForMonth(referenceMonth).stream()
                .map(envelope -> toView(envelope, referenceMonth))
                .sorted(Comparator.comparing(view -> view.envelopeModel().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvelopeView getViewById(UUID id, LocalDate referenceMonth) {
        return toView(getById(id), referenceMonth);
    }

    @Transactional(readOnly = true)
    public EnvelopeModel getById(UUID id) {
        return envelopeModelRepository
                .findWithAssociationsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envelope was not found."));
    }

    @Transactional
    public EnvelopeModel create(CreateEnvelopeRequest request) {
        EnvelopeModel envelope = new EnvelopeModel();
        apply(
                envelope,
                request.name(),
                request.type(),
                request.ownerMemberId(),
                request.categoryIds(),
                request.monthlyLimit());
        envelope.setCreatedInMonth(currentReferenceMonth());
        envelope.setActive(true);
        return envelopeModelRepository.save(envelope);
    }

    @Transactional
    public EnvelopeModel update(UUID id, UpdateEnvelopeRequest request) {
        EnvelopeModel envelope = getById(id);
        apply(
                envelope,
                request.name(),
                request.type(),
                request.ownerMemberId(),
                request.categoryIds(),
                request.monthlyLimit());
        return envelopeModelRepository.save(envelope);
    }

    @Transactional
    public EnvelopeModel archive(UUID id, ArchiveEnvelopeRequest request) {
        EnvelopeModel envelope = getById(id);
        if (request.archivedFromMonth().isBefore(envelope.getCreatedInMonth())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Archive month cannot be before the envelope creation month.");
        }
        envelope.setArchivedFromMonth(request.archivedFromMonth());
        envelope.setActive(false);
        return envelopeModelRepository.save(envelope);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listTransactions(UUID id, LocalDate referenceMonth) {
        return filterTransactions(getById(id), referenceMonth);
    }

    @Transactional(readOnly = true)
    public List<EnvelopeCategoryBreakdownItem> categoryBreakdown(UUID id, LocalDate referenceMonth) {
        List<Transaction> transactions = filterTransactions(getById(id), referenceMonth);
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet()
                .stream()
                .map(entry -> {
                    Transaction firstMatch = transactions.stream()
                            .filter(transaction ->
                                    transaction.getCategory().getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();
                    return new EnvelopeCategoryBreakdownItem(
                            firstMatch.getCategory().getId().toString(),
                            firstMatch.getCategory().getName(),
                            entry.getValue());
                })
                .sorted(Comparator.comparing(
                        EnvelopeCategoryBreakdownItem::categoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private EnvelopeView toView(EnvelopeModel envelope, LocalDate referenceMonth) {
        BigDecimal consumedAmount = filterTransactions(envelope, referenceMonth).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EnvelopeView(
                envelope, consumedAmount, envelope.getMonthlyLimit().subtract(consumedAmount));
    }

    private List<Transaction> filterTransactions(EnvelopeModel envelope, LocalDate referenceMonth) {
        List<Transaction> monthlyTransactions =
                transactionRepository.findByFilters(referenceMonth, null, null, null, null, null);

        if (envelope.getType() == EnvelopeType.ALLOWANCE) {
            UUID ownerId = envelope.getOwnerMember() == null
                    ? null
                    : envelope.getOwnerMember().getId();
            return monthlyTransactions.stream()
                    .filter(transaction -> transaction.getOwnershipType() == OwnershipType.INDIVIDUAL)
                    .filter(transaction -> transaction.getMember() != null)
                    .filter(transaction -> transaction.getMember().getId().equals(ownerId))
                    .toList();
        }

        Set<UUID> categoryIds =
                envelope.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return monthlyTransactions.stream()
                .filter(transaction -> transaction.getOwnershipType() == OwnershipType.SHARED)
                .filter(transaction ->
                        categoryIds.contains(transaction.getCategory().getId()))
                .toList();
    }

    private void apply(
            EnvelopeModel envelope,
            String name,
            EnvelopeType type,
            UUID ownerMemberId,
            List<UUID> categoryIds,
            BigDecimal monthlyLimit) {
        envelope.setName(name.trim());
        envelope.setType(type);
        envelope.setMonthlyLimit(monthlyLimit);

        if (type == EnvelopeType.ALLOWANCE) {
            if (ownerMemberId == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "Allowance envelopes require an owner member.");
            }
            FamilyMember owner = familyMemberRepository
                    .findById(ownerMemberId)
                    .filter(FamilyMember::isActive)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Family member was not found."));
            if (!owner.isAllowanceEnabled()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Allowance envelopes require a member with allowance enabled.");
            }
            if (envelope.getId() == null
                    && envelopeModelRepository.existsByOwnerMemberIdAndType(owner.getId(), EnvelopeType.ALLOWANCE)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "An allowance envelope already exists for this member.");
            }
            envelope.setOwnerMember(owner);
            envelope.setCategories(new LinkedHashSet<>());
            return;
        }

        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Global envelopes require at least one category.");
        }

        envelope.setOwnerMember(null);
        envelope.setCategories(categoryIds.stream()
                .map(categoryService::getById)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private LocalDate currentReferenceMonth() {
        return YearMonth.now().atDay(1);
    }
}
