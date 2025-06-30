package com.scratch;

import java.util.*;

/** Detects winning combinations in a generated matrix. Immutable and stateless. */
final class WinDetector {

  private final Config cfg;

  WinDetector(Config cfg) {
    this.cfg = Objects.requireNonNull(cfg);
  }

  /** Returns map of symbol → list of applied win rule IDs. */
  Map<String, List<String>> detect(List<List<String>> matrix) {
    Map<String, List<String>> wins = new HashMap<>();

    Map<String, Long> counts = symbolCounts(matrix);
    applySameSymbolRules(counts, wins);
    applyLinearRules(matrix, wins);
    return wins;
  }

  private Map<String, Long> symbolCounts(List<List<String>> m) {
    return m.stream()
        .flatMap(List::stream)
        .filter(cfg.standard()::containsKey)
        .collect(
            java.util.stream.Collectors.groupingBy(e -> e, java.util.stream.Collectors.counting()));
  }

  private void applySameSymbolRules(Map<String, Long> counts, Map<String, List<String>> acc) {
    cfg.winCombinations()
        .forEach(
            (id, rule) -> {
              if (rule.when() == When.SAME_SYMBOLS) {
                counts.forEach(
                    (sym, cnt) -> {
                      if (cnt >= rule.count()) addRule(acc, sym, id, rule);
                    });
              }
            });
  }

  private void applyLinearRules(List<List<String>> m, Map<String, List<String>> acc) {
    cfg.winCombinations()
        .forEach(
            (id, rule) -> {
              if (rule.when() == When.LINEAR_SYMBOLS && rule.coveredAreas() != null) {
                rule.coveredAreas()
                    .forEach(
                        area -> {
                          String first = get(m, area.get(0));
                          if (!cfg.standard().containsKey(first)) return;
                          boolean allMatch = area.stream().allMatch(c -> first.equals(get(m, c)));
                          if (allMatch) addRule(acc, first, id, rule);
                        });
              }
            });
  }

  private void addRule(Map<String, List<String>> acc, String sym, String ruleId, WinRule rule) {
    List<String> list = acc.computeIfAbsent(sym, k -> new ArrayList<>());
    String existing =
        list.stream()
            .filter(id -> cfg.winCombinations().get(id).group().equals(rule.group()))
            .findFirst()
            .orElse(null);
    if (existing != null) {
      if (rule.rewardMultiplier() > cfg.winCombinations().get(existing).rewardMultiplier()) {
        list.remove(existing);
        list.add(ruleId);
      }
    } else {
      list.add(ruleId);
    }
  }

  private static String get(List<List<String>> m, String rc) {
    String[] p = rc.split(":");
    return m.get(Integer.parseInt(p[0])).get(Integer.parseInt(p[1]));
  }
}
