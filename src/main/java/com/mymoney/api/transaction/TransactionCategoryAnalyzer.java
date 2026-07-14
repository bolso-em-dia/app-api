package com.mymoney.api.transaction;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryAnalyzer {

    public <T> List<CategoryAmount> analyzeByCategory(
            List<T> items,
            Function<T, String> categoryIdExtractor,
            Function<T, String> categoryNameExtractor,
            Function<T, BigDecimal> amountExtractor,
            Comparator<CategoryAmount> order) {
        return items.stream().collect(Collectors.groupingBy(categoryIdExtractor)).entrySet().stream()
                .map(entry -> {
                    var groupedItems = entry.getValue();
                    var total = groupedItems.stream().map(amountExtractor).reduce(BigDecimal.ZERO, BigDecimal::add);
                    var categoryName = categoryNameExtractor.apply(groupedItems.get(0));
                    return new CategoryAmount(entry.getKey(), categoryName, total);
                })
                .sorted(order)
                .toList();
    }

    public record CategoryAmount(String categoryId, String categoryName, BigDecimal amount) {}
}
