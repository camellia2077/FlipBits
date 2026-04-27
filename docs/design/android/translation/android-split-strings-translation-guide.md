# Android Split Strings Translation Rules

适用文件：

- `strings_common.xml`
- `strings_audio.xml`
- `strings_saved.xml`
- `strings_settings.xml`
- `strings_about.xml`
- `strings_validation.xml`

## Structure

- 英文 `values/strings_*.xml` 是唯一结构真源。
- 其他语言必须按英文这 6 个文件对齐。
- 已拆分语言直接改 `values-*/strings_*.xml`。
- 若文件里有 `<!-- CONTEXT: ... -->`，优先按注释理解，不只看英文词面。

## File Roles

- `strings_common.xml`：App 名、通用按钮、底部 tab。要求最短、最稳定。
- `strings_audio.xml`：Audio 页动作、播放模式、状态、结果、详情。要求像主流音乐 app。
- `strings_saved.xml`：Saved 页、导入、分享、重命名、删除、移动、文件夹。要求像媒体库或文件管理。
- `strings_settings.xml`：语言、主题、dual-tone、palette、flash style、about 入口。要求像系统设置页。
- `strings_about.xml`：作者、版本、GitHub、开源许可。要求信息型、简短。
- `strings_validation.xml`：校验、不可用、错误。要求短错误句，不暴露无必要实现细节。

## Hard Rules

- 术语跨文件必须一致：`Saved` / `Settings` / `Create` / `Read` / `Read result` / `Flash style` / `Dual-tone`
- 播放模式必须成组一致：`Normal` / `Random` / `Repeat one` / `Repeat all`
- `Read` 表示把音频读回文本结果，不是朗读。
- `flash / pro / ultra` 作为 mode 名，不翻模式 id。
- `High Gothic / Altum Gothicum` 是风格化语言名，不当普通句子翻。
- 短 UI 标签优先产品化、紧凑，不写成解释句。
- sample/example 文本优先自然语感，不逐词照搬英文。
- 风格化标题和专有名先保语义，不因形近音近改成别的概念。
- 音频、样式、设置类术语优先自然产品语言，避免临床化、实现细节化、状态位式表达。
- 同一概念在相关标签里出现时，译法保持一致。

## Length

- tab、按钮、chips、segmented labels：越短越好。
- `status_*`：短句优先。
- `subtitle` / `description`：可稍长，但不要翻成英文式长句。

## Android XML

- 文本最终写入 Android XML `<string>`。
- 不要输出：`\'` `\"` `\n` `\t` `\uXXXX`
- 需要标点时直接写字面字符。
- 保留占位符：`%1$s` `%2$d` `%%`

## Workflow

1. 先确认 key 属于哪个英文 `strings_*.xml`
2. 先看 `CONTEXT` 注释
3. 先保证术语和长度
4. 最后检查占位符、转义、跨文件一致性
