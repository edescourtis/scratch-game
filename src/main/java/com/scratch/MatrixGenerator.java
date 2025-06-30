package com.scratch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates a symbol matrix according to per-cell standard symbol probabilities and injects a
 * single bonus symbol chosen from the global bonus distribution. All randomness is supplied from
 * the outside for deterministic tests.
 */
final class MatrixGenerator {

  private final Config cfg;
  private final Random rng;
  private final CellPicker[][] pickers;
  private final CellPicker bonusPicker;

  MatrixGenerator(Config cfg, Random rng) {
    this.cfg = Objects.requireNonNull(cfg);
    this.rng = Objects.requireNonNull(rng);
    this.pickers = precomputePickers();
    this.bonusPicker = CellPicker.of(cfg.probabilities().bonusSymbols().symbols());
  }

  /**
   * Generates a fresh matrix according to the standard-symbol probabilities and injects random
   * bonus symbols. Returns both the matrix and the list of injected bonus symbols so callers
   * don't need to traverse the matrix to discover them.
   */
  GenerationResult generate() {
    List<List<String>> matrix =
        IntStream.range(0, cfg.rows())
            .mapToObj(
                r ->
                    IntStream.range(0, cfg.columns())
                        .mapToObj(c -> pickers[r][c].pick(rng))
                        .collect(Collectors.toList()))
            .collect(Collectors.toList());

    List<String> injected = applyBonusSymbols(matrix);
    return new GenerationResult(matrix, injected);
  }

  /** Injects 0‒N bonus symbols (skipping MISS). Returns list of those actually placed. */
  private List<String> applyBonusSymbols(List<List<String>> matrix) {
    if (bonusPicker == null) return List.of();

    int maxCells = cfg.rows() * cfg.columns();
    // Randomly decide how many bonuses to insert: 0 .. rows (heuristic)
    int toInsert = 1 + rng.nextInt(cfg.rows());
    java.util.Set<String> placedCoords = new java.util.HashSet<>();
    java.util.List<String> inserted = new java.util.ArrayList<>();

    for (int i = 0; i < toInsert && placedCoords.size() < maxCells; i++) {
      String bonus = bonusPicker.pick(rng);
      BonusSymbol b = cfg.bonus().get(bonus);
      if (b == null || b.impact() == Impact.MISS) {
        continue; // skip MISS or unknown
      }
      // find an unused coordinate to place bonus
      int attempts = 0;
      while (attempts < maxCells) {
        int r = rng.nextInt(cfg.rows());
        int c = rng.nextInt(cfg.columns());
        String key = r + ":" + c;
        if (placedCoords.add(key)) {
          matrix.get(r).set(c, bonus);
          inserted.add(bonus);
          break;
        }
        attempts++;
      }
    }
    return inserted;
  }

  private CellPicker[][] precomputePickers() {
    int r = cfg.rows(), c = cfg.columns();
    CellPicker[][] arr = new CellPicker[r][c];
    Map<String, Integer> fallback = cfg.probabilities().standardSymbols().get(0).symbols();
    cfg.probabilities()
        .standardSymbols()
        .forEach(w -> arr[w.row()][w.column()] = CellPicker.of(w.symbols()));
    for (int i = 0; i < r; i++)
      for (int j = 0; j < c; j++) if (arr[i][j] == null) arr[i][j] = CellPicker.of(fallback);
    return arr;
  }

  /** Helper for weighted random selection. */
  record CellPicker(String[] symbols, int[] cdf) {
    static CellPicker of(Map<String, Integer> w) {
      String[] s = w.keySet().toArray(String[]::new);
      int[] c = new int[s.length];
      int sum = 0;
      for (int i = 0; i < s.length; i++) {
        sum += w.get(s[i]);
        c[i] = sum;
      }
      return new CellPicker(s, c);
    }

    String pick(Random r) {
      int x = r.nextInt(cdf[cdf.length - 1]);
      int i = java.util.Arrays.binarySearch(cdf, x + 1);
      return symbols[i < 0 ? -i - 1 : i];
    }
  }
}

/** Immutable value object returned by {@link MatrixGenerator#generate()} */
record GenerationResult(List<List<String>> matrix, List<String> bonusSymbols) {}
