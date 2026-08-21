package com.settle.expense.strategy;

import com.settle.expense.SplitType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies;

    public SplitStrategyFactory(List<SplitStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(java.util.stream.Collectors.toMap(SplitStrategy::getSupportedType, s -> s));
    }

    public SplitStrategy getStrategy(SplitType splitType) {
        SplitStrategy strategy = strategies.get(splitType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported split type: " + splitType);
        }
        return strategy;
    }
}
