package com.scratch;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Computes the monetary reward given detected wins and bet amount. */
final class RewardCalculator {

  private final Config cfg;

  RewardCalculator(Config cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  double compute(double bet, Map<String, List<String>> wins) {
    return wins.entrySet().stream()
        .mapToDouble(
            e -> {
              double symbolMultiplier = cfg.standard().get(e.getKey()).rewardMultiplier();
              double combinedRuleMultiplier =
                  e.getValue().stream()
                      .map(id -> cfg.winCombinations().get(id).rewardMultiplier())
                      .reduce(1.0, (a, b) -> a * b);
              return bet * symbolMultiplier * combinedRuleMultiplier;
            })
        .sum();
  }
}
