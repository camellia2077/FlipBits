# Android Agent Read-Order

这是一页给 agent 的短索引：先决定当前改动属于哪一类，再展开最少量的文档。

## Default Start

1. 先看 [apps/audio_android/README.md](/C:/code/WaveBits/apps/audio_android/README.md) 的“快速定位 / 常见改动入口”。
2. 再按当前任务类型，只展开下面一个分支。
3. 如果改动跨了多个方向，再补读第二篇，不要一上来把所有 Android 文档全展开。

## Pick One Branch

### UI

适用场景：
- 改页面结构、组件职责、交互流、播放器布局
- 改 Audio / Saved / Settings 页入口或共享 UI helper

先读：
- [android-player-ui.md](/C:/code/WaveBits/docs/design/android/android-player-ui.md)

再按需读：
- [android-ui-structure.md](/C:/code/WaveBits/docs/architecture/android-ui-structure.md)

### Theme

适用场景：
- 改 dual-tone 配色、主题阵容、主题 token、预览/说明色
- 改 encode glyph、Settings 里的主题展示

先读：
- [android-dual-tone-theme.md](/C:/code/WaveBits/docs/design/android/android-dual-tone-theme.md)

再按需读：
- [android-player-ui.md](/C:/code/WaveBits/docs/design/android/android-player-ui.md)

### Translation

适用场景：
- 改 XML 文案、本地化结构、样例文本
- 构建被 translation key alignment 卡住
- 需要生成翻译任务 markdown 或排查语言 key 不对齐

先读：
- [android-translation-workflow.md](/C:/code/WaveBits/docs/design/android/android-translation-workflow.md)

再按需读：
- [android-translation-tooling-agent-index.md](/C:/code/WaveBits/docs/design/android/android-translation-tooling-agent-index.md)
- [android-localization-guidelines.md](/C:/code/WaveBits/docs/design/android/android-localization-guidelines.md)

### Native

适用场景：
- 改 JNI、native 音频编解码、WAV metadata、导出/导入链路
- 改 `@Keep`、混淆、native 反射访问、release-only 崩溃排查

先读：
- [android-native-strategy.md](/C:/code/WaveBits/docs/architecture/android-native-strategy.md)

再按需读：
- [android-app-architecture.md](/C:/code/WaveBits/docs/architecture/android-app-architecture.md)

## Minimal Read Combos

- 只改 Kotlin 业务逻辑：`README.md`
- 改页面和交互：`README.md` -> `android-player-ui.md`
- 改主题和配色：`README.md` -> `android-dual-tone-theme.md`
- 改 XML 文案或翻译：`README.md` -> `android-translation-workflow.md`
- 改 JNI / metadata / release-only native 问题：`README.md` -> `android-native-strategy.md`
- 改共享 UI 入口归属：`README.md` -> `android-ui-structure.md`

## When To Stop Reading

如果任务只是：
- 调一个已有参数
- 改一个已知入口的小 bug
- 补一个不涉及视觉规则的新分支

通常读完 `README.md` 和对应分支的第一篇文档就够了。

## Goal

这个索引的目标不是把规则再写一遍，而是帮助 agent 快速判断：
- 现在先看哪篇
- 哪些文档其实可以先不展开
- 什么时候需要从应用层继续下钻到架构层或工具层
