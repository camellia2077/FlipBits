# Android Translation Tooling Background

这篇文档不再作为 agent 入口索引。

当前职责只有一个：
- 解释 `audio_android` 为什么会额外维护一套 `android-translate` 工具链，而不是直接让 agent 逐份 XML 读写

真正入口统一是：
- 项目入口： [apps/audio_android/AGENTS.md](../../../apps/audio_android/AGENTS.md)
- 执行步骤： [.agent/workflows/translations/README.md](../../../.agent/workflows/translations/README.md)
- 工具定义： [tools/repo_tooling/android_translate/docs/README.md](../../../tools/repo_tooling/android_translate/docs/README.md)

## Why The Tool Exists

这套工具的核心目标是降低重复劳动，而不是把翻译判断写死在规则里。

主要问题有：
- 直接读取整份 XML token 成本高
- Android 字符串资源重复结构很多，噪音远大于有效上下文
- 英文基线和 localized 资源需要结构对齐
- 有些英文应保留，有些必须翻译，有些必须先补上下文

工具链的价值，是先把“结构、筛选、上下文、可执行载荷”整理好，再把真正需要判断的部分交给 agent 或人工。

## What The Tooling Separates

当前工具链实际上把几类问题拆开了：

- 结构对齐：
  - `key-alignment`
- 英文基线对照审校：
  - `compare`
  - `export-entries`
- 非英语里残留英文的发现与分桶：
  - `mixed-language`
  - `mixed-language-context-audit`
- 安全写回 XML：
  - `build-replacements`
  - `replace`

这样做的目的，是避免让每次翻译修改都从“重新读所有 XML”开始。

## Why JSON-First Matters

这套工具后来改成 JSON-first，不只是格式偏好，而是为了让 agent 的工作面更稳定：

- task JSON 比 markdown 更适合程序消费
- per-entry 字段能直接告诉 agent 这条更像什么问题
- prompt/profile 不需要反复全文塞入每个任务

所以现在的重点不是“产出更多说明文档”，而是让 task artifact 更接近可执行载荷。

## Relationship To Project Rules

这个工具不是通用独立产品，它在 `audio_android` 里要服从项目规则：

- `values/` 是英文基线真源
- `values-*` 是 localized target
- `CONTEXT` 注释属于资源侧上下文
- locked-term / mixed-language policy 属于工具侧单一数据源

因此工具定义和项目入口需要分开，但它们又必须对齐。

## What This Page Is Not

这篇文档不负责：
- 告诉你下一步该跑哪个 workflow
- 充当 CLI 手册
- 列命令速查表
- 代替项目入口文档

如果你现在是要实际做翻译修改，不应从这篇开始。

## Related Docs

- 项目入口：
  - [apps/audio_android/AGENTS.md](../../../apps/audio_android/AGENTS.md)
- workflow 入口：
  - [.agent/workflows/translations/README.md](../../../.agent/workflows/translations/README.md)
- 工具定义入口：
  - [tools/repo_tooling/android_translate/docs/README.md](../../../tools/repo_tooling/android_translate/docs/README.md)
- 工具架构：
  - [tools/repo_tooling/android_translate/docs/architecture.md](../../../tools/repo_tooling/android_translate/docs/architecture.md)
