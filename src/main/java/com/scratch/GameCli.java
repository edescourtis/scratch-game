package com.scratch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Command-line entry point for the Scratch Game.<br>
 *
 * <p>Uses a single static <code>ObjectMapper</code> instance – thread-safe after configuration and
 * recommended to avoid repeated instantiation overhead (<a
 * href="https://stackoverflow.com/questions/18611565">SO #18611565 – reuse ObjectMapper</a>).
 *
 * <p>All <code>System.exit</code> calls are confined to this CLI layer; library code throws
 * exceptions instead (see <a href="https://stackoverflow.com/questions/3715967">SO #3715967 – avoid
 * System.exit in libs</a>).
 */
public final class GameCli {

  /** Utility class – no instances allowed. */
  private GameCli() {
    throw new AssertionError("static-only");
  }

  private static final ObjectMapper MAPPER =
      new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  /**
   * Main entry point for the CLI application.
   *
   * @param args Command-line arguments. Use --help for usage.
   */
  public static void main(String[] args) {
    System.exit(run(args));
  }

  // package-private for testability
  static int run(String[] args) {
    try {
      if (args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
        printUsage();
        return 0;
      }

      Map<String, String> parsedArgs = parseArgs(args);
      Path configPath = Paths.get(parsedArgs.get("--config"));
      double betAmount = Double.parseDouble(parsedArgs.get("--bet_amount"));

      if (betAmount <= 0) {
        throw new IllegalArgumentException("bet must be > 0");
      }

      Config cfg = MAPPER.readValue(Files.readString(configPath), Config.class).withDefaults();
      cfg.validate(); // Validate configuration before using it
      GameEngine engine = new GameEngine(cfg, new SecureRandom());
      Result result = engine.play(betAmount);

      System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result));
      return 0;

    } catch (java.io.IOException | RuntimeException e) {
      System.err.println("Error: " + e.getMessage());
      return 1;
    }
  }

  /**
   * Parses CLI arguments of the form <code>--key value</code>. Flags can appear in any order and
   * the method tolerates additional, unknown flags so long as they follow the <code>--key value
   * </code> pattern. Duplicate flags will be overridden by the last occurrence (same behaviour as
   * GNU getopt).
   *
   * <p>The parser purposely avoids external dependencies (picocli, commons-cli, etc.) to keep the
   * binary small and the assignment self-contained.
   */
  private static Map<String, String> parseArgs(String[] args) {
    if (args.length == 0) {
      throw new IllegalArgumentException("No arguments provided. Use --help for usage.");
    }

    if (args.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Expected flag/value pairs. Found an uneven number of tokens (" + args.length + ").");
    }

    Map<String, String> result = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) {
      String key = args[i];
      String value = args[i + 1];

      if (!key.startsWith("--")) {
        throw new IllegalArgumentException("Unexpected argument: " + key);
      }
      if (value.startsWith("--")) {
        throw new IllegalArgumentException("Missing value for argument: " + key);
      }

      // normalise aliases
      String normalisedKey =
          switch (key) {
            case "--betting-amount", "--betting_amount" -> "--bet_amount";
            default -> key;
          };

      result.put(normalisedKey, value);
    }

    if (!result.containsKey("--config")) {
      throw new IllegalArgumentException("Missing required argument: --config");
    }
    if (!result.containsKey("--bet_amount")) {
      throw new IllegalArgumentException("Missing required argument: --bet_amount");
    }

    return result;
  }

  /** Prints usage information to the console. */
  private static void printUsage() {
    System.out.println(
        "Usage: scratch-game [--help] --config <configPath> --bet_amount|--betting-amount <betAmount>");
    System.out.println("Plays one round of the scratch matrix game.");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --config <configPath>     Path to JSON configuration file");
    System.out.println("  --bet_amount|--betting-amount <betAmount>  Positive bet amount");
    System.out.println("  --help, -h                Show this help message and exit");
  }
}
