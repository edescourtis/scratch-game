package com.scratch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings(
    value = "DM_CONVERT_CASE",
    justification = "Case conversion used only in assertion message comparison within tests")
class ConfigTest {

  private static Config createTestConfig() {
    Map<String, SymbolDef> symbols =
        Map.of(
            "A", new SymbolDef(5.0, "standard", null, null),
            "B", new SymbolDef(3.0, "standard", null, null),
            "10x", new SymbolDef(10.0, "bonus", "multiply_reward", null));

    List<CellWeights> standardProbs = List.of(new CellWeights(0, 0, Map.of("A", 1, "B", 1)));
    BonusWeights bonusProbs = new BonusWeights(Map.of("10x", 1));
    Probabilities probabilities = new Probabilities(standardProbs, bonusProbs);

    Map<String, WinRule> winRules =
        Map.of("same_symbol_3_times", new WinRule(1.0, When.SAME_SYMBOLS, 3, "same_symbols", null));

    return new Config(3, 3, symbols, probabilities, winRules);
  }

  @Test
  void validate_withValidConfig_doesNotThrow() {
    Config config = createTestConfig();
    assertDoesNotThrow(config::validate);
  }

  @Test
  void validate_withNullSymbols_throwsIllegalArgumentException() {
    Config config = createTestConfig();
    Config invalidConfig =
        new Config(
            config.rows(),
            config.columns(),
            null,
            config.probabilities(),
            config.winCombinations());
    Exception ex = assertThrows(IllegalArgumentException.class, invalidConfig::validate);
    assertTrue(ex.getMessage().contains("Configuration must define symbols"));
  }

  @Test
  void validate_withEmptySymbols_throwsIllegalArgumentException() {
    Config config = createTestConfig();
    Config invalidConfig =
        new Config(
            config.rows(),
            config.columns(),
            Map.of(),
            config.probabilities(),
            config.winCombinations());
    Exception ex = assertThrows(IllegalArgumentException.class, invalidConfig::validate);
    assertTrue(ex.getMessage().contains("Configuration must define symbols"));
  }

  @Test
  void validate_withNullProbabilities_throwsIllegalArgumentException() {
    Config config = createTestConfig();
    Config invalidConfig =
        new Config(
            config.rows(), config.columns(), config.symbols(), null, config.winCombinations());
    Exception ex = assertThrows(IllegalArgumentException.class, invalidConfig::validate);
    assertTrue(ex.getMessage().contains("Configuration must define probabilities"));
  }

  @Test
  void validate_withNullWinCombinations_throwsIllegalArgumentException() {
    Config config = createTestConfig();
    Config invalidConfig =
        new Config(config.rows(), config.columns(), config.symbols(), config.probabilities(), null);
    Exception ex = assertThrows(IllegalArgumentException.class, invalidConfig::validate);
    assertTrue(ex.getMessage().contains("Configuration must define win combinations"));
  }

  @Test
  void validate_withEmptyWinCombinations_throwsIllegalArgumentException() {
    Config config = createTestConfig();
    Config invalidConfig =
        new Config(
            config.rows(), config.columns(), config.symbols(), config.probabilities(), Map.of());
    Exception ex = assertThrows(IllegalArgumentException.class, invalidConfig::validate);
    assertTrue(ex.getMessage().contains("Configuration must define win combinations"));
  }

  @Test
  void validate_withOutOfBoundsCoordinate_throwsIllegalArgumentException() {
    Map<String, SymbolDef> symbols = Map.of("A", new SymbolDef(5.0, "standard", null, null));
    List<CellWeights> standardProbs = List.of(new CellWeights(0, 0, Map.of("A", 1)));
    BonusWeights bonusProbs = new BonusWeights(Map.of("MISS", 1));
    Probabilities probs = new Probabilities(standardProbs, bonusProbs);

    // define a coordinate outside 3x3 matrix (column index 3)
    List<List<String>> coveredAreas = List.of(List.of("0:3"));
    Map<String, WinRule> winRules =
        Map.of("bad_rule", new WinRule(1.0, When.LINEAR_SYMBOLS, null, "linear", coveredAreas));

    Config cfg = new Config(3, 3, symbols, probs, winRules);

    Exception ex = assertThrows(IllegalArgumentException.class, cfg::validate);
    assertTrue(ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("out of bounds"));
  }

  @Test
  void validate_withMissingStandardSymbolProbabilities_throwsIllegalArgumentException() {
    Map<String, SymbolDef> symbols = Map.of("A", new SymbolDef(5.0, "standard", null, null));
    // Empty standard symbol probabilities list
    Probabilities probs = new Probabilities(List.of(), new BonusWeights(Map.of("MISS", 1)));

    Map<String, WinRule> winRules =
        Map.of("dummy", new WinRule(1.0, When.SAME_SYMBOLS, 3, "same_symbols", null));

    Config cfg = new Config(3, 3, symbols, probs, winRules);
    Exception ex = assertThrows(IllegalArgumentException.class, cfg::validate);
    assertTrue(ex.getMessage().contains("standard symbol probabilities"));
  }
}
