# Agent Guide: Responsibility Risk JSON

## Purpose

This document explains how an agent should interpret the Python responsibility-risk JSON fields written to files such as:

- [scan_py.json](/C:/code/WaveBits/tools/scripts/loc/logs/scan_py.json)
- [scan_py_responsibility_test.json](/C:/code/WaveBits/tools/scripts/loc/logs/scan_py_responsibility_test.json)

The goal is to help agents turn the scan output into better refactoring decisions.

These numbers are diagnostic hints, not refactoring targets.

Do not optimize a file just to make a specific count smaller.
Use the fields to understand what kinds of responsibilities are mixed together.

## Reading Order

When an agent reads one matched file entry, use this order:

1. `priority`
2. `summary`
3. `dominant_risks`
4. `suggestion`
5. the detailed counts

This order matters.

- `priority` tells you how urgent the file is
- `summary` tells you the mixed-responsibility shape in plain language
- `dominant_risks` tells you which category is driving the warning
- `suggestion` gives a safe first split direction
- the counts are supporting evidence only

## Field Meaning

### Core conclusion fields

- `path`
  - The file being analyzed.

- `score`
  - A heuristic risk score.
  - Higher means “more likely to contain mixed responsibilities”, not “must be split exactly this much”.

- `priority`
  - `P0` to `P3`.
  - Use this to choose inspection order.

- `summary`
  - Short natural-language diagnosis of the main risk pattern.
  - This should be the first explanation an agent reads.

- `dominant_risks`
  - A list of the main risk categories.
  - Prefer these over raw counts when deciding the split direction.

- `suggestion`
  - A single direction-oriented recommendation.
  - Treat it as a starting point, not a mandatory transformation.

### Supporting evidence fields

- `lines`
  - File size signal only.

- `state_signal_hits`
  - Stateful or side-effect-oriented patterns such as `self.`, `subprocess`, `asyncio`, `os.environ`.

- `top_level_composables`
  - For Python this actually means top-level `def/class` symbol count.

- `role_kinds`
  - Naming-role hints such as `Service`, `Parser`, `Writer`, `Loader`.

- `mode_branch_hits`
  - Branches over mode-like selectors such as `mode`, `kind`, `type`, `match/case`.

- `io_kind_count`
  - How many IO / side-effect categories appear in one file.
  - Examples: filesystem, console, process, network, environment, serialization.

- `rule_helper_count`
  - Density of top-level rule helpers and rule-like constants.
  - Examples: `validate_*`, `resolve_*`, `normalize_*`, regex constants, all-caps constants.

- `responsibility_verb_kind_count`
  - How many responsibility verb groups appear at top level.
  - Examples:
    - read/load/parse
    - validate/check
    - resolve/normalize
    - apply/write/update
    - print/render/format

- `command_layer_leak_hits`
  - Specific to files under `commands/`.
  - Higher values suggest the commands layer still contains too much core logic.

## Dominant Risk Categories

### `command_layer_leak`

Meaning:

- A file under `commands/` still contains too many rule helpers or deep implementation details.

Likely split direction:

- move parsing / validating / resolving / matching helpers into `core/`
- keep `commands/` focused on orchestration and output shaping

### `io_surface_breadth`

Meaning:

- One file touches too many IO or side-effect surfaces.

Likely split direction:

- separate core logic from file writes, console output, subprocess calls, or environment handling

### `rule_helper_density`

Meaning:

- One file contains too many rules, regexes, validators, or resolver helpers.

Likely split direction:

- move rule definitions and low-level validation into a dedicated core/helper module

### `mixed_responsibility_verbs`

Meaning:

- The file simultaneously reads, validates, resolves, modifies, and renders.

Likely split direction:

- split by action family:
  - reading/parsing
  - validation
  - mutation/application
  - presentation/output

### `stateful_side_effects`

Meaning:

- Stateful or side-effect patterns are dense.

Likely split direction:

- inspect whether execution coordination and pure logic can be separated

### `mode_branching`

Meaning:

- The file may be carrying too many mode-specific flows.

Likely split direction:

- consider extracting per-mode handlers or narrowing the branching surface

## What Agents Should Not Do

Do not treat these values as strict goals:

- `io_kind_count`
- `rule_helper_count`
- `responsibility_verb_kind_count`
- `command_layer_leak_hits`

Bad behavior:

- trying to reduce `rule_helper_count` mechanically without clarifying ownership
- moving random functions only because the count is high
- overfitting a refactor to make numbers smaller

Good behavior:

- use the counts as evidence
- use `summary`, `dominant_risks`, and `suggestion` to decide the direction
- confirm the code-level boundary after reading the actual file

## Suggested Agent Workflow

1. Sort by `priority`, then `score`.
2. Read `summary` and `dominant_risks`.
3. Read the target source file.
4. Confirm whether the warning is real.
5. Use `suggestion` as the first split hypothesis.
6. Refactor around ownership boundaries, not metric minimization.
7. Re-run the scanner afterward to confirm the file became clearer, but do not chase perfect numbers.

## Example Interpretation

Example:

```json
{
  "path": "commands/apply_translation_replacements.py",
  "score": 6,
  "priority": "P2",
  "summary": "中度风险：commands 层混入底层规则 helper、规则常量、校验和解析 helper 过密",
  "dominant_risks": [
    "command_layer_leak",
    "rule_helper_density"
  ],
  "suggestion": "优先把底层规则、校验和解析 helper 下沉到 core，命令层只保留编排和结果输出。",
  "lines": 401,
  "io_kind_count": 2,
  "rule_helper_count": 6,
  "responsibility_verb_kind_count": 3,
  "command_layer_leak_hits": 7
}
```

Correct takeaway:

- the important part is not that `command_layer_leak_hits == 7`
- the important part is that the file is probably mixing:
  - command orchestration
  - validation helpers
  - XML/JSON rule logic

The refactor target should be:

- move deep helpers into `core/`
- keep the command file focused on flow coordination and result reporting
