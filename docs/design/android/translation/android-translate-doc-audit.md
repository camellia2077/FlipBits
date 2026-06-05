# Android Translate Doc Audit

日期：

- `2026-06-05`

## 目标

把 `tools/repo_tooling/android_translate/docs` 里混在一起的两类内容拆开：

- 项目级使用说明
- 工具定义 / CLI contract / 内部架构

当前仓库只有一个使用者，所以这次不保留兼容入口，不做过渡层。

## 结论

### 保留在 `tools/repo_tooling/android_translate/docs/`

这些文档属于工具定义，继续留在工具目录：

- `README.md`
- `architecture.md`
- `cli_contract.md`

理由：

- 它们描述的是命令面、JSON contract、内部结构和工具边界
- 这些内容更接近实现，而不是项目内的日常操作指南

### 迁移到 `docs/design/android/translation/`

这些内容是“怎么在本仓库里使用这套工具”，应该进项目 docs：

- `sop.md`
- `apply_translation_replacements.md`
- `check_mixed_language.md`
- `check_translation_key_alignment.md`
- `compare_translation_quality.md`
- `dump_xml_text_md.md`
- `note.md`

迁移后的合并结果：

- [android-translate-command-guide.md](android-translate-command-guide.md)
- [README.md](README.md)

理由：

- 原文多数是执行说明、命令示例和推荐工作流
- 多篇之间存在明显重复
- 部分示例和说明已经过时

### 删除而不是继续保留的内容

- 工具目录下重复的 usage 文档入口
- 零散的临时 note 型文档

理由：

- 这些内容和 workflow / project docs 重叠
- 继续保留只会造成“同一命令有两套说法”

## 本次发现的过时点

### 已修正

- `fix-resource-escapes` 的说明此前仍把 `d'accento -> d\'accento` 当成预期
- 现在已经改为和当前实现一致的 Android-safe quoted literal 口径

### 需要后续继续关注

- `architecture.md` 仍有少量“典型工作流”叙述，偏使用说明，后续可以再收紧
- `lint-baseline.json` 中的 `escaped_apostrophe` 历史项，和当前自动修复后的规范含义需要重新审视

## 最终边界

今后建议固定成：

- `docs/design/android/translation/`
  - 项目级使用说明
  - workflow 背景
  - 命令怎么用
  - 本地化和翻译规则

- `tools/repo_tooling/android_translate/docs/`
  - CLI contract
  - JSON contract
  - 内部架构
  - 实现边界
