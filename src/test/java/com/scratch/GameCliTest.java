package com.scratch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameCliTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  private Path tempConfigFile;

  @BeforeEach
  void setUp() throws IOException {
    // Redirect System.out and System.err to capture CLI output
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));

    // Create a temporary config file in the local directory for reliable access
    tempConfigFile = Path.of("test-config.json");
    String configJson =
        """
        {
          "rows": 3, "columns": 3,
          "symbols": {
            "A": {"reward_multiplier": 5.0, "type": "standard"},
            "MISS": {"type": "bonus", "impact": "miss"}
          },
          "probabilities": {
            "standard_symbols": [
              {"column": 0, "row": 0, "symbols": {"A": 1}}
            ],
            "bonus_symbols": {
              "symbols": {"MISS": 1}
            }
          },
          "win_combinations": {
            "same_3": {"reward_multiplier": 2.0, "when": "same_symbols", "count": 3, "group": "same"}
          }
        }
        """;
    Files.writeString(tempConfigFile, configJson);
  }

  @AfterEach
  void tearDown() throws IOException {
    // Restore original streams and delete the temporary file
    System.setOut(originalOut);
    System.setErr(originalErr);
    Files.deleteIfExists(tempConfigFile);
  }

  @Test
  void main_withValidArgs_printsJsonResult() {
    String[] args = {"--config", tempConfigFile.toString(), "--bet_amount", "100"};

    int exitCode = GameCli.run(args);

    String output = outContent.toString(StandardCharsets.UTF_8);
    // A successful run should print a JSON object and return exit code 0
    assertEquals(0, exitCode);
    assertTrue(output.trim().startsWith("{"));
    assertTrue(output.contains("\"matrix\""));
    assertTrue(output.contains("\"reward\""));
    assertTrue(errContent.toString(StandardCharsets.UTF_8).isEmpty());
  }

  @Test
  void main_withHelpFlag_printsUsageInfo() {
    int exitCode = GameCli.run(new String[] {"--help"});
    assertEquals(0, exitCode);
    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("Usage: scratch-game"));
    assertTrue(output.contains("--config"));
    assertTrue(output.contains("--bet_amount"));
  }

  @Test
  void main_withShortHelpFlag_printsUsageInfo() {
    int exitCode = GameCli.run(new String[] {"-h"});
    assertEquals(0, exitCode);
    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("Usage: scratch-game"));
  }

  @Test
  void main_withInvalidArgCount_printsError() {
    int exitCode = GameCli.run(new String[] {"--config", "file.json"});
    assertEquals(1, exitCode);
    String error = errContent.toString(StandardCharsets.UTF_8);
    assertTrue(error.startsWith("Error:"));
    assertTrue(error.contains("Missing required argument") || error.contains("Expected flag"));
  }

  @Test
  void main_withMissingConfigArg_printsError() {
    int exitCode = GameCli.run(new String[] {"--foo", "bar", "--bet_amount", "100"});
    assertEquals(1, exitCode);
    String error = errContent.toString(StandardCharsets.UTF_8);
    assertTrue(error.contains("Error: Missing required argument: --config"));
  }

  @Test
  void main_withMissingBetAmountArg_printsError() {
    int exitCode = GameCli.run(new String[] {"--config", "file.json", "--foo", "bar"});
    assertEquals(1, exitCode);
    String error = errContent.toString(StandardCharsets.UTF_8);
    assertTrue(error.contains("Error: Missing required argument: --bet_amount"));
  }

  @Test
  void main_withNonNumericBet_printsError() {
    String[] args = {"--config", tempConfigFile.toAbsolutePath().toString(), "--bet_amount", "abc"};
    int exitCode = GameCli.run(args);
    assertEquals(1, exitCode);
    String error = errContent.toString(StandardCharsets.UTF_8);
    assertTrue(error.contains("Error: For input string: \"abc\""));
  }

  @Test
  void main_withNegativeBet_printsError() {
    String[] args = {
      "--config", tempConfigFile.toAbsolutePath().toString(), "--bet_amount", "-100"
    };
    int exitCode = GameCli.run(args);
    assertEquals(1, exitCode);
    String error = errContent.toString(StandardCharsets.UTF_8);
    assertTrue(error.contains("Error: bet must be > 0"));
  }

  @Test
  void main_withInaccessibleConfigFile_printsError() {
    String[] args = {"--config", "/path/to/nonexistent/config.json", "--bet_amount", "100"};
    int exitCode = GameCli.run(args);
    assertEquals(1, exitCode);
    String error = errContent.toString(StandardCharsets.UTF_8);
    assertTrue(error.contains("Error:"));
    // Check for file not found details, specific message depends on OS
    assertTrue(error.contains("nonexistent") || error.contains("No such file or directory"));
  }

  @Test
  void main_withValidArgs_outputsParsableJson() throws Exception {
    String[] args = {"--config", tempConfigFile.toString(), "--bet_amount", "100"};

    int exitCode = GameCli.run(args);

    assertEquals(0, exitCode);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(outContent.toString(StandardCharsets.UTF_8));

    assertNotNull(root.get("matrix"));
    assertTrue(root.get("matrix").isArray());
    assertNotNull(root.get("reward"));
  }

  @Test
  void main_withBettingAmountAlias_works() {
    String[] args = {"--config", tempConfigFile.toString(), "--betting-amount", "150"};

    int exitCode = GameCli.run(args);
    assertEquals(0, exitCode);
    String output = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("\"reward\""));
  }
}
