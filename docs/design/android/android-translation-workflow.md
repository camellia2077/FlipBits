# Android Translation Workflow Background

这篇文档不再作为翻译入口页。

当前职责只有两个：
- 解释为什么 Android 项目需要单独的 XML 翻译工作流
- 记录这套工作流与 app 结构、Gradle 校验、英文基线拆分之间的关系

真正入口统一是：
- 项目入口： [apps/audio_android/AGENTS.md](/C:/code/WaveBits/apps/audio_android/AGENTS.md)
- 执行步骤： [.agent/workflows/translations/README.md](/C:/code/WaveBits/.agent/workflows/translations/README.md)
- 工具定义： [tools/repo_tooling/android_translate/docs/README.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/README.md)

## Why This Exists

Android 这一侧的翻译问题不是单纯“把英文改成另一种语言”。

它同时涉及：
- 英文 `values/` 作为结构真源
- `values-*` 与英文文件结构对齐
- `strings_*.xml` 与 `audio_samples_*.xml` 两类文本职责不同
- Android 构建链里需要尽早发现缺 key、漏翻、错误保留词和资源转义问题

所以项目里保留了一套独立的翻译工具链，而不是让 agent 或人工直接逐份 XML 盲改。

## Project-Specific Constraints

这套流程在 `audio_android` 里有几个项目内约束：

- 英文基线按职责拆在多个 `strings_*.xml` 中，而不是单一大文件
- 样例文本 `audio_samples_*.xml` 需要和普通 UI 文案分开处理
- `strings-add` 只写英文基线，不默认复制到 `values-*`
- Gradle 构建会接入 `key-alignment`
- 多语言修订默认走 JSON-first task artifacts，而不是先靠 markdown 或直接读 XML

这些约束决定了项目不能把翻译流程退回到“手工找文件 + 手工复制英文”。

## Relationship To Gradle

Android 构建会把翻译结构检查接进默认链路。

这意味着：
- 新增英文 key 后，缺失的 localized key 会尽早暴露
- 文件拆分后的结构漂移不会长期静默存在
- 翻译工作流不是完全独立于构建系统的外部流程

这也是为什么本项目把翻译视为资源结构维护的一部分，而不是纯文案后处理。

## Relationship To App Code

翻译工作流和几类代码位置有直接关系：

- 英文与 localized XML 资源：
  - `apps/audio_android/app/src/main/res/values/`
  - `apps/audio_android/app/src/main/res/values-*/`
- 样例文本提供与语言切换：
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/data/AndroidSampleInputTextProvider.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/SampleInputSessionUpdater.kt`
- dual-tone 主题文案和样例：
  - `BrandThemeCatalog.kt`
  - 对应 `strings_settings.xml` / `audio_samples_*`

所以翻译不是孤立的数据清洗，而是和 app 的文案展示、默认内容、主题系统直接相连。

## What This Page Is Not

这篇文档不负责：
- 告诉你下一步该跑哪个命令
- 充当 workflow 首页
- 解释完整 CLI contract
- 重复 locale policy 或 prompt 规则

这些内容分别归：
- workflow
- docs
- prompt/profile/policy

## Related Background Docs

- 语义与术语规则：
  - [android-localization-guidelines.md](/C:/code/FlipBits/docs/design/android/android-localization-guidelines.md)
- 英文拆分资源说明：
  - [android-split-strings-translation-guide.md](/C:/code/FlipBits/docs/design/android/translation/android-split-strings-translation-guide.md)
- 工具架构：
  - [tools/repo_tooling/android_translate/docs/architecture.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/architecture.md)
