package com.scratch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.*;
import java.util.Locale;

/**
 * Game configuration record. Holds all parameters for a game session.<br>
 *
 * <p>Implements <em>immutable value objects</em> via Java&nbsp;17 {@code record}s – a concise,
 * final, field-based syntax recommended for domain models<br>
 * (see <a href="https://baeldung.com/java-immutable-object">Baeldung Immutable Objects</a>).
 *
 * <p>Symbol discriminators such as {@code type} and {@code when} are mapped to enums rather than
 * strings, providing compile-time safety and IDE assist (<a
 * href="https://ahdak.github.io/blog/effective-java-part-5">Effective Java Item 34 – Enums</a>).
 */
record Config(
    int columns,
    int rows,
    Map<String, SymbolDef> symbols,
    Probabilities probabilities,
    Map<String, WinRule> winCombinations) {

  /**
   * Returns a config with default values for columns/rows if not set.
   *
   * @return Config with defaults applied.
   */
  Config withDefaults() {
    return new Config(
        columns == 0 ? 3 : columns, rows == 0 ? 3 : rows, symbols, probabilities, winCombinations);
  }

  /**
   * Validates that the configuration has all required fields and valid coordinates. Throws
   * IllegalArgumentException if invalid.
   */
  void validate() {
    if (symbols == null || symbols.isEmpty()) {
      throw new IllegalArgumentException("Configuration must define symbols");
    }
    if (probabilities == null) {
      throw new IllegalArgumentException("Configuration must define probabilities");
    }
    if (probabilities.standardSymbols() == null || probabilities.standardSymbols().isEmpty()) {
      throw new IllegalArgumentException("Configuration must define standard symbol probabilities");
    }
    if (probabilities.bonusSymbols() == null) {
      throw new IllegalArgumentException("Configuration must define bonus symbol probabilities");
    }
    if (winCombinations == null || winCombinations.isEmpty()) {
      throw new IllegalArgumentException("Configuration must define win combinations");
    }
    // Validate coordinate references in win rules
    winCombinations.forEach(
        (ruleId, rule) -> {
          if (rule.coveredAreas() != null) {
            rule.coveredAreas()
                .forEach(
                    area -> {
                      area.forEach(
                          coordinate -> validateCoordinate(coordinate, rows, columns, ruleId));
                    });
          }
        });
  }

  /**
   * Validates a coordinate string against matrix bounds.
   *
   * @param coordinate The coordinate string (e.g. "0:1").
   * @param rows Number of rows in the matrix.
   * @param cols Number of columns in the matrix.
   * @param ruleId The rule ID for error context.
   */
  private void validateCoordinate(String coordinate, int rows, int cols, String ruleId) {
    try {
      String[] parts = coordinate.split(":");
      if (parts.length != 2) {
        throw new IllegalArgumentException(
            "Invalid coordinate format in rule " + ruleId + ": " + coordinate);
      }
      int row = Integer.parseInt(parts[0]);
      int col = Integer.parseInt(parts[1]);
      if (row < 0 || row >= rows || col < 0 || col >= cols) {
        throw new IllegalArgumentException(
            "Coordinate out of bounds in rule "
                + ruleId
                + ": "
                + coordinate
                + " (matrix is "
                + rows
                + "x"
                + cols
                + ")");
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid coordinate format in rule " + ruleId + ": " + coordinate, e);
    }
  }

  /**
   * Returns a map of all standard symbols in the config.
   *
   * @return Map of symbol name to StandardSymbol.
   */
  Map<String, StandardSymbol> standard() {
    Map<String, StandardSymbol> result = new HashMap<>();
    for (var entry : symbols.entrySet()) {
      if (SymbolType.STANDARD.getJsonValue().equals(entry.getValue().type())) {
        result.put(entry.getKey(), new StandardSymbol(entry.getValue().rewardMultiplier()));
      }
    }
    return result;
  }

  /**
   * Returns a map of all bonus symbols in the config.
   *
   * @return Map of symbol name to BonusSymbol.
   */
  Map<String, BonusSymbol> bonus() {
    Map<String, BonusSymbol> result = new HashMap<>();
    for (var entry : symbols.entrySet()) {
      if (SymbolType.BONUS.getJsonValue().equals(entry.getValue().type())) {
        Impact impact = Impact.valueOf(entry.getValue().impact().toUpperCase(Locale.ROOT));
        result.put(
            entry.getKey(),
            new BonusSymbol(entry.getValue().rewardMultiplier(), entry.getValue().extra(), impact));
      }
    }
    return result;
  }
}

/**
 * Enum for symbol categories (<em>standard</em> vs&nbsp;<em>bonus</em>). Using an enum instead of
 * string literals eliminates illegal constants and enables exhaustive <code>switch</code> checks
 * (<a href="https://stackoverflow.com/questions/5092015">SO #5092015 – Enum vs String</a>).
 */
enum SymbolType {
  STANDARD("standard"),
  BONUS("bonus");

  private final String jsonValue;

  SymbolType(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  /** Returns the JSON value for this symbol type. */
  public String getJsonValue() {
    return jsonValue;
  }

  /**
   * Parses a SymbolType from its JSON value.
   *
   * @param jsonValue The JSON value.
   * @return The SymbolType.
   */
  public static SymbolType fromJsonValue(String jsonValue) {
    for (SymbolType type : values()) {
      if (type.jsonValue.equals(jsonValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown symbol type: " + jsonValue);
  }
}

/** Record for symbol definition (standard or bonus). */
record SymbolDef(Double rewardMultiplier, String type, String impact, Integer extra) {}

/** Record for standard symbol (reward multiplier only). */
record StandardSymbol(double rewardMultiplier) {}

/** Enum for bonus impact types. */
enum Impact {
  MULTIPLY_REWARD,
  EXTRA_BONUS,
  MISS
}

/** Record for bonus symbol (reward multiplier, extra, impact). */
record BonusSymbol(Double rewardMultiplier, Integer extra, Impact impact) {
  /**
   * Applies this bonus symbol's impact to the given reward.
   *
   * @param reward The base reward.
   * @return The reward after applying the bonus impact.
   */
  public double applyImpact(double reward) {
    return switch (impact) {
      case MULTIPLY_REWARD -> reward * rewardMultiplier;
      case EXTRA_BONUS -> reward + extra;
      case MISS -> reward;
    };
  }
}

/** Record for bonus symbol weights (probabilities). */
record BonusWeights(Map<String, Integer> symbols) {}

/** Record for symbol probabilities (standard and bonus). */
record Probabilities(List<CellWeights> standardSymbols, BonusWeights bonusSymbols) {}

/** Record for cell weights (probabilities for each cell). */
record CellWeights(int row, int column, Map<String, Integer> symbols) {}

/** Enum for win rule types (when). */
enum When {
  SAME_SYMBOLS("same_symbols"),
  LINEAR_SYMBOLS("linear_symbols");

  private final String jsonValue;

  When(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  /** JSON serialization value */
  @JsonValue
  public String getJsonValue() {
    return jsonValue;
  }

  /** Factory for deserialization from JSON string */
  @JsonCreator
  public static When fromJsonValue(String jsonValue) {
    for (When w : values()) {
      if (w.jsonValue.equalsIgnoreCase(jsonValue)) return w;
    }
    throw new IllegalArgumentException("Unknown when value: " + jsonValue);
  }
}

/** Record for win rule definition. */
record WinRule(
    double rewardMultiplier,
    When when,
    Integer count,
    String group,
    List<List<String>> coveredAreas) {}

/** Record for game result (matrix, reward, applied wins, bonus symbol). */
record Result(
    List<List<String>> matrix,
    double reward,
    Map<String, List<String>> applied,
    String appliedBonusSymbol) {}
