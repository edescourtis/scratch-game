package com.scratch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import com.scratch.GenerationResult;

/**
 * Core game logic for the Scratch Game.<br>
 *
 * <p>Randomness is injected via constructor rather than using <code>new Random()</code> inside the
 * class, allowing deterministic, seeded unit-tests and enabling <code>SecureRandom</code> for
 * production if needed (<a href="https://stackoverflow.com/questions/71188966">SO #71188966 –
 * inject RNG for tests</a>).
 *
 * <p>Weighted random symbol selection is implemented by pre-computing a cumulative-distribution
 * array and performing binary-search lookup – reducing complexity from <code>O(n)</code> to <code>
 * O(log n)</code> (<a href="https://stackoverflow.com/questions/4511331">SO #4511331 – cumulative
 * weights</a>).
 */
final class GameEngine {

  private final Config cfg;

  private final MatrixGenerator generator;
  private final WinDetector detector;
  private final RewardCalculator calculator;

  /**
   * Constructs a new GameEngine with the given configuration and random source.
   *
   * @param cfg The game configuration.
   * @param rng The random number generator.
   */
  GameEngine(Config cfg, Random rng) {
    this.cfg = Objects.requireNonNull(cfg);

    this.generator = new MatrixGenerator(cfg, rng);
    this.detector = new WinDetector(cfg);
    this.calculator = new RewardCalculator(cfg);
  }

  /**
   * Plays a single round of the game with the given bet amount.
   *
   * @param bet The betting amount.
   * @return The result of the game round.
   */
  Result play(double bet) {
    validateBettingAmount(bet);
    GenerationResult gen = generator.generate();
    List<List<String>> matrix = gen.matrix();

    Map<String, List<String>> wins = detector.detect(matrix);
    double reward = calculator.compute(bet, wins);

    // Use first bonus symbol if any (additional bonuses share same impact rules — spec note²)
    String bonusSymbol = gen.bonusSymbols().isEmpty() ? null : gen.bonusSymbols().get(0);

    if (reward > 0 && bonusSymbol != null && !isMissSymbol(bonusSymbol)) {
      reward = applyBonus(reward, cfg.bonus().get(bonusSymbol));
      return new Result(matrix, reward, wins, bonusSymbol);
    }

    return new Result(matrix, reward, wins, null);
  }

  /**
   * Validates that the betting amount is positive.
   *
   * @param bet The betting amount.
   */
  private void validateBettingAmount(double bet) {
    if (bet <= 0) {
      throw new IllegalArgumentException("Betting amount must be positive, got: " + bet);
    }
  }

  /**
   * Checks if a symbol is a "miss" type bonus (no effect).
   *
   * @param symbol The symbol name.
   * @return true if the symbol is a MISS bonus, false otherwise.
   */
  private boolean isMissSymbol(String symbol) {
    BonusSymbol bonusSymbol = cfg.bonus().get(symbol);
    return bonusSymbol != null && bonusSymbol.impact() == Impact.MISS;
  }

  /**
   * Applies a bonus symbol's impact to the reward.
   *
   * @param reward The base reward.
   * @param b The bonus symbol.
   * @return The reward after applying the bonus.
   */
  private static double applyBonus(double reward, BonusSymbol b) {
    return b.applyImpact(reward);
  }

  private String extractBonusSymbol(List<List<String>> matrix) {
    return null; // unused after refactor
  }
}
