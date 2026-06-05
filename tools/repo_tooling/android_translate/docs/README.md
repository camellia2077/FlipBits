# Android Translate Tool Definition Docs

这个目录现在只保留工具定义文档，不再承担项目级使用说明。

这里应该放的是：

- CLI / command surface
- JSON contract
- output directories
- internal architecture

不要把这里当成日常操作入口。

项目内实际使用说明统一看：

- 项目入口： [apps/audio_android/AGENTS.md](../../../../apps/audio_android/AGENTS.md)
- workflow 入口： [.agent/workflows/translations/README.md](../../../../.agent/workflows/translations/README.md)
- 项目级命令说明： [docs/design/android/translation/README.md](../../../../docs/design/android/translation/README.md)

## Start Here

- Stable CLI and task JSON contract: [cli_contract.md](cli_contract.md)
- Tool architecture: [architecture.md](architecture.md)

## Main Commands

命令本身仍然是这些：

- `compare`
- `replace`
- `replace-batch`
- `key-alignment`
- `mixed-language`
- `mixed-language-context-audit`
- `untranslated-equals-english`
- `fix-resource-escapes`
- `dump-xml-md`
- `unused-keys`

但“怎么在本仓库里使用这些命令”不再在这里展开，统一看项目 docs。

## JSON-First Contract

Generated task artifacts are JSON-first.

- Read `*.task.json` before markdown.
- Use markdown only when the JSON payload lacks enough inspection detail.
- Prefer `execution_contract`, `locale_profile`, `style_profile`, and per-entry execution fields before re-parsing raw XML.

## Output Directories

All generated tool outputs live under `temp/translations/`.

- Reviews: `temp/translations/reviews`
- Key alignment: `temp/translations/key_alignment`
- Mixed language: `temp/translations/mixed_language`
- Mixed language audit: `temp/translations/mixed_language_audit`
- Unused key reports: `temp/translations/unused_keys`
- Entry exports: `temp/translations/entry_exports`
- XML dump: `temp/translations/xml_dump`

## Directory Map

- `run.py`
  - Unified CLI entrypoint
- `commands/`
  - Command implementations
- `core/`
  - Shared parsing, JSON, reporting, replacement, and policy helpers
- `prompts/`
  - Shared prompt/profile definitions
- `docs/`
  - Tool-definition-only docs
