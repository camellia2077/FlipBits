# Android Translate Command Guide

这篇文档只讲一件事：在 `audio_android` 里，日常该怎么用 `android-translate`。

命令入口统一是：

```powershell
python tools/run.py android-translate <command>
```

资源根目录默认是：

- `apps/audio_android/app/src/main/res`

## 常用命令

### 1. `compare`

用途：

- 生成英文基线 vs 本地化资源的审校产物
- 给人工或 LLM 做 review / replacement 输入

常用例子：

```powershell
python tools/run.py android-translate compare
python tools/run.py android-translate compare --lang ja --text-type app_text --group strings_settings --json-output
```

适用场景：

- 想审校某个语言的 `strings_*.xml`
- 想抽查某个 faction 的 sample text
- 想生成 JSON-first review 任务

### 2. `replace`

用途：

- 按 replacement JSON 定点修改 localized XML

常用例子：

```powershell
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --auto-fix-json --json-output
```

适用场景：

- 已经有 `dir + items[name/find/replace]` 形式的修改建议
- 想安全写回 XML，而不是手工改资源

### 3. `key-alignment`

用途：

- 检查 `values/` 英文基线和 `values-*` 本地化资源是否结构对齐

常用例子：

```powershell
python tools/run.py android-translate key-alignment
python tools/run.py android-translate key-alignment --json-output
```

适用场景：

- 新增了英文 key
- 拆分了 `strings_*.xml`
- 怀疑某些语言缺 key / 多 key / 多文件

### 4. `mixed-language`

用途：

- 找出非英文语言里疑似残留的跨语种文本

常用例子：

```powershell
python tools/run.py android-translate mixed-language
python tools/run.py android-translate mixed-language-context-audit --lang ko --json-output
```

适用场景：

- 检查 CJK 语言里是否混入了英文
- 检查非 CJK 语言里是否混入了 CJK 字符串
- 做翻译清理前的批量筛查

### 5. `fix-resource-escapes`

用途：

- 修复 Android XML `<string>` 里的高风险资源转义写法
- 尤其适合处理会触发 `aapt` / `mergeDebugResources` 失败的单引号问题

常用例子：

```powershell
python tools/run.py android-translate fix-resource-escapes --res-dir apps/audio_android/app/src/main/res
python tools/run.py android-translate fix-resource-escapes --quiet
python tools/run.py android-translate fix-resource-escapes apps/audio_android/app/src/main/res/values-it/strings_settings.xml
```

当前自动修复能力：

- raw ASCII apostrophe：`d'accento`
- legacy escaped apostrophe：`d\'accento`

当前会统一修成 Android-safe 的带双引号字符串字面量，例如：

- `d'accento` -> `"d'accento"`
- `d\'accento` -> `"d'accento"`

注意：

- 这个命令不会猜测性修复语义不明确的坏 `\u` 序列
- 这类问题应先人工确认原意，再决定是否改文本

### 6. `dump-xml-md`

用途：

- 低噪音导出 XML 文本，便于快速肉眼检查

常用例子：

```powershell
python tools/run.py android-translate dump-xml-md --lang ko --text-type sample_text --group exquisite_fall
python tools/run.py android-translate dump-xml-md --lang ko --text-type sample_text --group exquisite_fall --with-en
```

适用场景：

- 不想走完整 compare review
- 只想快速看某个语言/某组 XML 当前文本

## 推荐收尾

做完翻译或 XML 修复后，默认跑这一组：

```powershell
python tools/run.py android-translate key-alignment --json-output
python tools/run.py android-translate lint --json-output
cd apps/audio_android
.\gradlew.bat app:assembleDebug
```

## 边界

这篇文档只讲项目里的实际使用方式，不重复：

- CLI exit code / JSON contract
- 内部模块架构
- 底层实现入口

这些继续看：

- [tools/repo_tooling/android_translate/docs/cli_contract.md](../../../../../tools/repo_tooling/android_translate/docs/cli_contract.md)
- [tools/repo_tooling/android_translate/docs/architecture.md](../../../../../tools/repo_tooling/android_translate/docs/architecture.md)
