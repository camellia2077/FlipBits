# Agent Guide: Responsibility Risk Reports

## Purpose

This document explains how an agent should interpret responsibility-risk JSON and Markdown fields written to files such as:

- [scan_py.json](../logs/scan_py.json)
- [scan_py_responsibility_test.json](../logs/scan_py_responsibility_test.json)

The goal is to help agents turn the scan output into better refactoring decisions.

## Scope Reports

Architecture-oriented reports are written by commands such as:

```powershell
python tools/scripts/loc/run.py --scope audio_api --responsibility-risk
python tools/scripts/loc/run.py --scope audio_android --responsibility-risk
```

A scope report groups related code under one root object and keeps one `parts` entry per language. Each part preserves the existing language report shape (`lang`, `scan`, `results`, and language-specific responsibility fields). Read the scope summary first, then inspect each part using the same language-specific rules below.

Scope baseline diffs use `part + kind + path` as the identity. This prevents a same-named C++ and Kotlin file from being collapsed into one diff entry.

Scope-specific refactor guidance is fixed documentation under `../docs/scopes/`:

- `audio_api.md`
- `audio_core.md`
- `audio_io.md`
- `audio_runtime.md`
- `audio_android.md`
- `audio_cli.md`
- `audio_web.md`

The scanner does not copy language guidance or scope guides into log directories. Read the scope guide first, then use the matching language guide under `languages/` for language-level extraction principles.

These numbers are diagnostic hints, not refactoring targets.

Do not optimize a file just to make a specific count smaller.
Use the fields to understand what kinds of responsibilities are mixed together.

## Reading Order

When an agent reads one matched file entry, use this order:

1. `priority`
2. `summary`
3. `dominant_risks`
4. `suggestion`
5. `next_action`
6. `confidence`
7. `decision`
8. Markdown-only `stop_signal`
9. Markdown/JSON `move_sets`
10. Markdown-only `Suggested Extraction Candidates`
11. `lines`
12. the detailed counts

This order matters.

- `priority` tells you how urgent the file is
- `summary` tells you the mixed-responsibility shape in plain language
- `dominant_risks` tells you which category is driving the warning
- `suggestion` gives a safe first split direction
- `next_action` gives a conservative first move
- `stop_signal` tells you whether to continue, pause, or manually review
- `move_sets` lists helper groups that should move together as one migration package
- `Suggested Extraction Candidates` turns hotspots into possible boundaries with validation hints
- `lines` keeps the file-size signal visible without letting it dominate the diagnosis
- the counts are supporting evidence only

## Field Meaning

### Core conclusion fields

- `path`
  - The file being analyzed.

- `score`
  - A heuristic risk score.
  - Higher means “more likely to contain mixed responsibilities”, not “must be split exactly this much”.

- `confidence`
  - Evidence confidence for the heuristic warning: `low`, `medium`, or `high`.
  - It measures evidence breadth, not architectural certainty.

- `decision`
  - Initial triage decision: `inspect` or `refactor_candidate`.
  - It is a prioritization hint; the agent must still confirm the source-level boundary.

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

- `move_sets`
  - C++ responsibility reports include this structured list when the scanner can group helpers by owner.
  - Each set names a target boundary, the helper names and line ranges that should move together, the reason, and the validation command.
  - Prefer this over moving one small helper at a time.

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

## Markdown Agent Sections

Detailed Markdown reports include extra sections intended for agents:

- `False Positive Notes`
  - Explains why a file may still be flagged even when further splitting is not useful.
  - Example: Compose files naturally contain `remember` and mode branches.

- `Responsibility Clusters`
  - Groups hotspots into likely ownership areas such as `dialog_import_export`, `canvas_visual_runtime`, `state_orchestration`, or `follow_annotation_ui`.
  - Use this to choose a coherent boundary rather than moving isolated functions randomly.

- `Move Sets`
  - Lists concrete migration packages.
  - Use this when planning C++ module-first extraction: choose one row, move the listed helpers together, then run the validation shown in that row.
  - For test files, treat this as a fixture-owner hint rather than an automatic split instruction.

- `Suggested Extraction Candidates`
  - Lists candidate owner, line range, suggested file boundary, risk, and validation.
  - Treat this as a shortlist, not an instruction to apply every row.

- `stop_signal`
  - `continue` means one small extraction is probably still worthwhile.
  - `pause` means the file may still score high, but further splitting should wait for a named behavior change.
  - `review manually` means the report lacks a clear low-risk boundary.

- `validation_hints`
  - Suggests the smallest useful compile/test command after the extraction.
  - Prefer these hints before running broad checks.

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
- ignoring a `pause` stop signal just because the file still has score 5
- applying every suggested extraction candidate in one turn

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
6. Check `stop_signal`.
7. If it says `continue`, choose one extraction candidate with a clear boundary.
8. Refactor around ownership boundaries, not metric minimization.
9. Run the validation hint.
10. Re-run the scanner afterward to confirm the file became clearer, but do not chase perfect numbers.

When a previous scan JSON is available, pass it with `--baseline` and use `diff.summary` plus `diff.entries` to verify whether the refactor changed the intended files and risk fields.

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
