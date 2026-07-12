package com.mymoney.api.transaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryAnalyzer {

    public List<CategoryAmount> analyzeByCategory(
            List<Transaction> transactions,
            Function<Transaction, BigDecimal> amountExtractor,
            Comparator<CategoryAmount> order) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory().getId()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Transaction> items = entry.getValue();
                    BigDecimal total = items.stream().map(amountExtractor).reduce(BigDecimal.ZERO, BigDecimal::add);
                    String categoryName = items.get(0).getCategory().getName();
                    return new CategoryAmount(entry.getKey().toString(), categoryName, total);
                })
                .sorted(order)
                .toList();
    }

    public record CategoryAmount(String categoryId, String categoryName, BigDecimal amount) {}
}
