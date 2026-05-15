# Rust Unsafe Usage Guide

This directory contains tools for scanning and auditing `unsafe` usage in the WaveBits codebase.

## Objective

The core goal is to **shrink `unsafe` usage to a few auditable FFI boundaries**, rather than allowing it to scatter throughout business logic.

### Why not zero unsafe?
Since `apps/audio_cli/rust/` interacts with `libs/audio_api` and `libs/audio_io` via the **C ABI**, `unsafe` cannot be eliminated entirely. Rust must use `unsafe` blocks to declare and call external C functions and to handle raw memory buffers returned by the native core.

## Audit Philosophy

We categorize `unsafe` usage into two main types to help maintain code quality:

1.  **FFI Boundaries (Clean)**: Minimal `unsafe` blocks that purely wrap an external function call or a raw pointer conversion. These are easier to audit and verify for safety invariants.
2.  **Scattered Logic (High Risk)**: `unsafe` blocks that contain complex logic, control flow, or are located deep within business modules. These should be refactored and moved into dedicated bridge modules.

## Scanner Tool

Use `unsafe_scanner.py` to generate an audit report.

### Usage
```bash
python tools/scripts/rust/unsafe_scanner.py [root_dir] [--output output_file]
```
- `root_dir`: The directory to scan (default: `apps/audio_cli/rust/src`).
- `--output`: Path to save the report (default: `temp/unsafe_usage_report.txt`).

### Report Labels
- **[B] Boundary**: Looks like a clean FFI or memory boundary.
- **[S] Scattered**: Contains complex logic or multiple statements. These are primary targets for refactoring.

## Categories of Unsafe Usage

| Category | Description | Example |
| :--- | :--- | :--- |
| `extern` | FFI function declarations. | `unsafe extern "C" { ... }` |
| `ffi-call` | Invocations of external functions. | `unsafe { bag_start_job(...) }` |
| `raw-slice` | Converting raw pointers to Rust slices. | `slice::from_raw_parts(...)` |
| `cstr` | Handling C-style strings. | `CStr::from_ptr(...)` |
| `drop-impl` | Implementing `Drop` for resource cleanup. | `impl Drop for ...` |
| `ptr-deref` | Raw pointer dereferencing. | `let x = *ptr;` |
| `other` | Unclassified unsafe usage. | |
