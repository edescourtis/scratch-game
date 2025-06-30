# Product Requirements Document (PRD) - Scratch Game

## 1. Overview

**Purpose:**  
Define the complete requirements and scope for a CLI-based Scratch Game assignment that generates a matrix from symbols based on probabilities and evaluates winning combinations to determine rewards.

**Stakeholders:**  

- Development candidates implementing the assignment
- Hiring team evaluating the implementation

## 2. Environment & Technical Constraints

- **JDK:** ≥ 1.8
- **Build Tool:** Maven or Gradle
- **Allowed Libraries:**  
  - JSON serialization/deserialization libraries (e.g., Jackson, Gson)
  - Testing frameworks (e.g., JUnit, TestNG)
- **Disallowed:** High-level frameworks (e.g., Spring, Micronaut, etc.)
- **Architecture:** CLI-based application with no GUI requirements

## 3. Functional Requirements

### 3.1 Configuration System

The application must load a JSON configuration file containing the following structure:

```json
{
  "columns": 3,
  "rows": 3,
  "symbols": {
    "A": { "reward_multiplier": 5, "type": "standard" },
    "B": { "reward_multiplier": 3, "type": "standard" },
    "C": { "reward_multiplier": 2.5, "type": "standard" },
    "D": { "reward_multiplier": 2, "type": "standard" },
    "E": { "reward_multiplier": 1.2, "type": "standard" },
    "F": { "reward_multiplier": 1, "type": "standard" },
    "10x": { "reward_multiplier": 10, "type": "bonus", "impact": "multiply_reward" },
    "5x": { "reward_multiplier": 5, "type": "bonus", "impact": "multiply_reward" },
    "+1000": { "extra": 1000, "type": "bonus", "impact": "extra_bonus" },
    "+500": { "extra": 500, "type": "bonus", "impact": "extra_bonus" },
    "MISS": { "type": "bonus", "impact": "miss" }
  },
  "probabilities": {
    "standard_symbols": [
      {
        "column": 0,
        "row": 0,
        "symbols": { "A": 1, "B": 2, "C": 3, "D": 4, "E": 5, "F": 6 }
      }
    ],
    "bonus_symbols": {
      "symbols": { "10x": 1, "5x": 2, "+1000": 3, "+500": 4, "MISS": 5 }
    }
  },
  "win_combinations": {
    "same_symbol_3_times": {
      "reward_multiplier": 1,
      "when": "same_symbols",
      "count": 3,
      "group": "same_symbols"
    }
  }
}
```

#### 3.1.1 Configuration Field Specifications

| Field Name | Type | Default | Description |
|------------|------|---------|-------------|
| `columns` | integer | 3 | Number of columns in the matrix (OPTIONAL) |
| `rows` | integer | 3 | Number of rows in the matrix (OPTIONAL) |
| `symbols` | object | - | Map of symbol definitions (standard and bonus) |
| `symbols.{X}.reward_multiplier` | number | - | Multiplier applied to betting amount |
| `symbols.{X}.type` | string | - | Either "standard" or "bonus" |
| `symbols.{X}.extra` | integer | - | [Bonus only] Flat amount added to reward |
| `symbols.{X}.impact` | string | - | [Bonus only] "multiply_reward", "extra_bonus", or "miss" |
| `probabilities.standard_symbols` | array | - | Per-cell probability weights for standard symbols |
| `probabilities.standard_symbols[].column` | integer | - | Column index |
| `probabilities.standard_symbols[].row` | integer | - | Row index |
| `probabilities.standard_symbols[].symbols` | object | - | Map of symbol to probability weight |
| `probabilities.bonus_symbols` | object | - | Global probability weights for bonus symbols |
| `win_combinations` | object | - | Map of winning combination rules |

### 3.2 Symbol Types

#### 3.2.1 Standard Symbols

Standard symbols determine win/loss status based on winning combinations:

| Symbol | Reward Multiplier |
|--------|-------------------|
| A | 5 |
| B | 3 |
| C | 2.5 |
| D | 2 |
| E | 1.2 |
| F | 1 |

#### 3.2.2 Bonus Symbols

Bonus symbols apply only when at least one winning combination is achieved:

| Symbol | Action | Effect |
|--------|---------|---------|
| 10x | multiply_reward | Multiply final reward by 10 |
| 5x | multiply_reward | Multiply final reward by 5 |
| +1000 | extra_bonus | Add 1000 to final reward |
| +500 | extra_bonus | Add 500 to final reward |
| MISS | miss | No effect |

### 3.3 Win Combinations

#### 3.3.1 Same Symbol Count Rules (MANDATORY)

| Rule Name | Count | Reward Multiplier | Group |
|-----------|-------|-------------------|-------|
| same_symbol_3_times | 3 | 1 | same_symbols |
| same_symbol_4_times | 4 | 1.5 | same_symbols |
| same_symbol_5_times | 5 | 2 | same_symbols |
| same_symbol_6_times | 6 | 3 | same_symbols |
| same_symbol_7_times | 7 | 5 | same_symbols |
| same_symbol_8_times | 8 | 10 | same_symbols |
| same_symbol_9_times | 9 | 20 | same_symbols |

#### 3.3.2 Linear Pattern Rules (OPTIONAL - Extra Points)

**Horizontal Lines:**

```json
"same_symbols_horizontally": {
  "reward_multiplier": 2,
  "when": "linear_symbols",
  "group": "horizontally_linear_symbols",
  "covered_areas": [
    ["0:0", "0:1", "0:2"],
    ["1:0", "1:1", "1:2"],
    ["2:0", "2:1", "2:2"]
  ]
}
```

**Vertical Lines:**

```json
"same_symbols_vertically": {
  "reward_multiplier": 2,
  "when": "linear_symbols",
  "group": "vertically_linear_symbols",
  "covered_areas": [
    ["0:0", "1:0", "2:0"],
    ["0:1", "1:1", "2:1"],
    ["0:2", "1:2", "2:2"]
  ]
}
```

**Left-to-Right Diagonal:**

```json
"same_symbols_diagonally_left_to_right": {
  "reward_multiplier": 5,
  "when": "linear_symbols",
  "group": "ltr_diagonally_linear_symbols",
  "covered_areas": [
    ["0:0", "1:1", "2:2"]
  ]
}
```

**Right-to-Left Diagonal:**

```json
"same_symbols_diagonally_right_to_left": {
  "reward_multiplier": 5,
  "when": "linear_symbols",
  "group": "rtl_diagonally_linear_symbols",
  "covered_areas": [
    ["0:2", "1:1", "2:0"]
  ]
}
```

### 3.4 Game Flow Requirements

#### 3.4.1 Matrix Generation

1. For each cell (row, column):
   - Select a standard symbol based on per-cell probability weights
   - Independently select a bonus symbol based on global probability weights
   - If bonus symbol is not "MISS", place it in the cell; otherwise use the standard symbol

#### 3.4.2 Probability Calculation

- Probability = (symbol_weight / sum_of_all_weights_in_cell) × 100%
- If a cell's probability configuration is missing, use the configuration from [0,0]

#### 3.4.3 Win Evaluation

1. Count occurrences of each standard symbol in the matrix
2. Apply same-symbol count rules where applicable
3. Check linear pattern rules for matching sequences
4. Apply group constraints (maximum one winning combination per group)

#### 3.4.4 Reward Calculation Rules

**Multiple Wins for Same Symbol (Multiply):**

```
reward = bet_amount × symbol_reward_multiplier × win_combination_1_multiplier × win_combination_2_multiplier
```

**Multiple Winning Symbols (Sum):**

```
total_reward = symbol_1_reward + symbol_2_reward + ... + symbol_n_reward
```

**Bonus Application:**

- Only applies if total_reward > 0
- `multiply_reward`: total_reward × bonus_multiplier
- `extra_bonus`: total_reward + bonus_extra_amount
- `miss`: no change

### 3.5 Input/Output Specifications

#### 3.5.1 CLI Input Format

```bash
java -jar scratch-game.jar --config config.json --betting-amount 100
```

| Parameter | Type | Description |
|-----------|------|-------------|
| --config | string | Path to JSON configuration file |
| --betting-amount | number | Betting amount (positive number) |

#### 3.5.2 Output Format

```json
{
  "matrix": [
    ["A", "A", "B"],
    ["A", "+1000", "B"],
    ["A", "A", "B"]
  ],
  "reward": 6600,
  "applied_winning_combinations": {
    "A": ["same_symbol_5_times", "same_symbols_vertically"],
    "B": ["same_symbol_3_times", "same_symbols_vertically"]
  },
  "applied_bonus_symbol": "+1000"
}
```

| Field Name | Type | Description |
|------------|------|-------------|
| matrix | array[array[string]] | Generated 2D matrix of symbols |
| reward | number | Final calculated reward amount |
| applied_winning_combinations | object | Map of symbol to list of applied win rule names |
| applied_bonus_symbol | string\|null | Applied bonus symbol or null if MISS/none |

### 3.6 Example Scenarios

#### 3.6.1 Lost Game Example

**Input:**

```bash
java -jar scratch-game.jar --config config.json --betting-amount 100
```

**Output:**

```json
{
  "matrix": [
    ["A", "B", "C"],
    ["E", "B", "5x"],
    ["F", "D", "C"]
  ],
  "reward": 0,
  "applied_winning_combinations": {},
  "applied_bonus_symbol": null
}
```

*Note: No winning combinations found, so bonus symbol is not applied despite being present.*

#### 3.6.2 Won Game Example

**Input:**

```bash
java -jar scratch-game.jar --config config.json --betting-amount 100
```

**Output:**

```json
{
  "matrix": [
    ["A", "B", "C"],
    ["E", "B", "10x"],
    ["F", "D", "B"]
  ],
  "reward": 3000,
  "applied_winning_combinations": {
    "B": ["same_symbol_3_times"]
  },
  "applied_bonus_symbol": "10x"
}
```

**Calculation:**

```
Base reward: 100 (bet) × 3 (symbol B multiplier) × 1 (3-times rule) = 300
Bonus applied: 300 × 10 (10x bonus) = 3000
```

## 4. Non-Functional Requirements

### 4.1 Code Quality

- **Testability:** Core logic implemented as pure functions
- **Maintainability:** Clean, readable code following SOLID principles
- **Build:** No compilation errors, all tests passing

### 4.2 Performance

- Matrix generation and reward calculation should complete within reasonable time for typical game sizes
- Memory usage should be efficient for standard configurations

### 4.3 Error Handling

- Graceful handling of invalid JSON configurations
- Clear error messages for missing or malformed input
- Validation of required configuration fields

## 5. Technical Assumptions

### 5.1 Configuration Assumptions

- CLI parameters are provided in the exact format specified
- JSON configuration file is well-formed and accessible
- Betting amount is a positive number
- Missing cell probabilities inherit from position [0,0]

### 5.2 Game Logic Assumptions

- Only one bonus symbol can be applied per game round
- Bonus symbols can appear in any cell of the matrix
- Group constraints ensure maximum one win rule per group applies
- Standard symbols in final matrix (after bonus replacement) determine wins

### 5.3 Probability Assumptions

- Probability weights are positive integers
- Total probability weights per cell determine relative likelihood
- Bonus symbol selection is independent of standard symbol selection per cell

## 6. Acceptance Criteria

### 6.1 Mandatory Features

- Load and parse JSON configuration
- Generate matrix based on probability weights
- Implement all same-symbol count rules (3-9 times)
- Calculate rewards using specified formulas
- Apply bonus symbols when wins exist
- Output correct JSON format
- CLI interface with specified parameters

### 6.2 Optional Features (Extra Points)

- Horizontal line pattern detection
- Vertical line pattern detection  
- Left-to-right diagonal pattern detection
- Right-to-left diagonal pattern detection
- Comprehensive unit test coverage
- Clean, functional programming style implementation

### 6.3 Quality Gates

- No compilation errors
- All unit tests passing
- Code follows clean coding principles
- Proper separation of concerns
- Minimal external dependencies

