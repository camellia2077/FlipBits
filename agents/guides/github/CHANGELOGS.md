---
description: Agent 专用 GitHub Changelog 写法
---

# GitHub Changelogs

本指南用于维护面向用户的 `CHANGELOG.md` / `CHANGELOG.zh-CN.md`。详细工程历史仍写在 `docs/history/**/v*/x.y.z.md`。

## 文件关系

- `CHANGELOG.md`：英文用户向 changelog。
- `CHANGELOG.zh-CN.md`：中文用户向 changelog。
- 两个文件应表达同一组版本事实，但不要求逐字直译。
- 每个文件顶部保留指向另一个语言版本的链接。

## 信息来源

优先使用对应版本 history：

- Android 示例：`docs/history/presentation/android/v*/x.y.z.md`
- libs 或其他包同理使用对应 history 目录

Changelog 是 history 的用户向摘要，不是工程明细索引。

## 结构

每个版本按新到旧排列：

```md
## vX.Y.Z

<可选：1 段版本总述>

### Added / 新增功能
* <用户可感知新增能力>

### Improved / 体验改进
* <用户可感知体验优化>

### Fixed / 问题修复
* <用户可感知修复>
```

规则：

- 英文文件使用 `Added` / `Improved` / `Fixed`。
- 中文文件使用 `新增功能` / `体验改进` / `问题修复`。
- 没有内容的分类删除。
- 列表统一使用 `* `。
- 不使用过深标题。

## 写作要求

- 面向用户，不面向实现。
- 合并同类改动，避免把 history 原文逐条复制。
- 可以保留用户可见名称，例如 `Voice`、`Clip`、`Live`、`Single track`、`Dual track`、`Binharic`。
- 避免文件路径、类名、函数名和测试名。
- 避免空话，例如“优化体验”“修复若干问题”。
- 不写未验证的性能、稳定性或兼容性承诺。

## 更新流程

1. 先确认对应版本 history 已完成。
2. 从 history 提炼用户可见能力、体验变化和修复。
3. 先写一种语言，再补另一种语言。
4. 对照两份 changelog，确认版本号、分类和事实一致。
5. 删除只属于内部实现、测试、工具链或文档维护的内容。

## 与 GitHub Release 的区别

- `CHANGELOG.md` / `CHANGELOG.zh-CN.md` 可以保留多个版本，适合长期浏览。
- GitHub Release 只写单个版本，应该更短、更像发布页摘要。
- Release 可以从 changelog 再压缩，不应比 changelog 更长。
