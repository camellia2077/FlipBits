# Translate Tooling Overview

## 这套工具是干什么的

这套 `tools/scripts/android/translate` 工具主要服务 Android 文本翻译和润色流程，目标是：

- 从英文基线和各语言资源生成可审校的 Markdown
- 帮你发现缺翻译、混杂语言、key 不对齐之类的问题
- 让 LLM 或人工只输出“小范围修改建议”
- 安全地把这些修改写回 Android `values-*` XML

它不是一个“自动翻译系统”，而是一套 **审校 + 定点替换** 工作流。

当前最重要的特点：

- 英文只作为 review 参考
- replace 不靠英文定位
- replace 依赖 `dir + items[name + find + replace]`
- 优先支持人类手工搬运 JSON 的省钱 SOP

## 典型工作流

1. 运行 `compare` 生成 review markdown
2. 从 `temp/ai_translation_reviews` 里挑目标语言的 md
3. 把 md 丢给网页端 AI 或人工审校
4. 让对方输出 JSON：

```json
{
  "dir": "values-it",
  "items": [
    {
      "name": "audio_sample_example_key",
      "find": "current substring",
      "replace": "improved substring"
    }
  ]
}
```

5. 保存到 `replacements.json`
6. 运行 `replace`

完整细节见 [sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)。

## 文件职责

### 入口

- [run.py](/C:/code/WaveBits/tools/scripts/android/translate/run.py)
  - 统一命令入口
  - 子命令分发：`compare` / `replace` / `mixed-language` / `key-alignment`

- [run.cmd](/C:/code/WaveBits/tools/scripts/android/translate/run.cmd)
  - Windows 下的通用入口包装

- [replace.cmd](/C:/code/WaveBits/tools/scripts/android/translate/replace.cmd)
  - Windows 下的普通 `replace` 快捷入口

- [replace_auto_fix_json.cmd](/C:/code/WaveBits/tools/scripts/android/translate/replace_auto_fix_json.cmd)
  - Windows 下的 `replace --auto-fix-json` 快捷入口

### Compare / Review

- [compare_translation_quality.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/compare_translation_quality.py)
  - 生成英文 vs 本地化语言的 review markdown
  - 如果你要改：
    - review 输出字段
    - 按语言/阵容/文件筛选逻辑
    - review 目录结构
    - English-only review 的组织方式
    - 就来这里

- [translation_review_prompts.py](/C:/code/WaveBits/tools/scripts/android/translate/prompts/translation_review_prompts.py)
  - 放给 LLM 的 prompt 文本
  - 如果你要改：
    - JSON 输出格式
    - 审校规则
    - 语气约束
    - 就来这里

### Replace

- [apply_translation_replacements.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/apply_translation_replacements.py)
  - 核心 replace 逻辑
  - 负责：
    - 读取 JSON
    - 校验 schema
    - 解析 dir / items / name / find / replace
    - 修改 XML
    - 输出 diff
    - 触发 smoke check
  - 如果你要改：
    - JSON schema
    - replace 定位规则
    - 校验条件
    - 替换策略
    - 就来这里

- [replacement_json_preflight.py](/C:/code/WaveBits/tools/scripts/android/translate/core/replacement_json_preflight.py)
  - replace 前的 JSON 预检和高置信度自动修复
  - 如果你要改：
    - JSON auto-fix 范围
    - JSON 语法错误提示
    - 就来这里

- [android_string_text.py](/C:/code/WaveBits/tools/scripts/android/translate/core/android_string_text.py)
  - Android string 的转义 / 反转义 / 高风险字符串检查
  - 如果你要改：
    - `\'`、`\u`、`@`、`?`、反斜杠等资源文本处理
    - 就来这里

- [android_resource_smoke_check.py](/C:/code/WaveBits/tools/scripts/android/translate/core/android_resource_smoke_check.py)
  - 跑 `:app:mergeDebugResources`
  - 如果你要改：
    - replace 后的资源级 smoke check 行为
    - 就来这里

- [replacement_entries.py](/C:/code/WaveBits/tools/scripts/android/translate/core/replacement_entries.py)
  - replace JSON schema 解析
  - 负责：
    - 顶层 `dir + items` 解析
    - `name / find / replace` 字段校验
    - placeholder / manual escape 拦截

- [xml_string_replacement.py](/C:/code/WaveBits/tools/scripts/android/translate/core/xml_string_replacement.py)
  - string 级 XML 定位与替换
  - 负责：
    - 按 `dir` 扫描翻译相关 XML
    - 在目录内按 `name` 唯一定位 string
    - 执行 `find` / `replace`
    - XML 与 Android string 风险校验

### 检查类工具

- [check_mixed_language.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/check_mixed_language.py)
  - 检查目标语言中是否混入不该出现的别的语言
  - 如果你要改：
    - 白名单
    - 非拉丁语言检测规则
    - ASCII / Pro 例外规则
    - 就来这里

- [check_translation_key_alignment.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/check_translation_key_alignment.py)
  - 检查 localized keys 是否是 English keys 的子集
  - 如果你要改：
    - 缺 key / 多 key 判断逻辑
    - Pro ASCII 例外规则
    - 报告粒度
    - 就来这里

### 共用底层

- [translation_paths.py](/C:/code/WaveBits/tools/scripts/android/translate/core/translation_paths.py)
  - 路径、语言标签、文件分类、sample 阵容归类
  - 如果你要改：
    - 资源根目录
    - 文件分类规则
    - faction 识别
    - 就来这里

- [translation_resources.py](/C:/code/WaveBits/tools/scripts/android/translate/core/translation_resources.py)
  - 资源仓库和 XML 读取
  - 如果你要改：
    - XML 读取方式
    - `ResourceFile` 结构
    - sample length 推断
    - 就来这里

- [translation_reporting.py](/C:/code/WaveBits/tools/scripts/android/translate/core/translation_reporting.py)
  - Markdown 报告块结构和写文件逻辑
  - 如果你要改：
    - report 输出格式
    - report block 组织形式
    - 就来这里

- [translation_common.py](/C:/code/WaveBits/tools/scripts/android/translate/core/translation_common.py)
  - 兼容导出层
  - 新代码尽量不要继续往这里加职责

## 常见修改去哪里

### 1. 想让 review markdown 多输出一个字段

去：

- [compare_translation_quality.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/compare_translation_quality.py)
- 如涉及通用格式，再看 [translation_reporting.py](/C:/code/WaveBits/tools/scripts/android/translate/core/translation_reporting.py)

### 2. 想改 LLM 返回 JSON 的格式

去：

- [translation_review_prompts.py](/C:/code/WaveBits/tools/scripts/android/translate/prompts/translation_review_prompts.py)
- [apply_translation_replacements.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/apply_translation_replacements.py)
- [templates/replacements.template.json](/C:/code/WaveBits/tools/scripts/android/translate/templates/replacements.template.json)

这三个要一起看。

### 3. 想增加 replace 的安全校验

去：

- [apply_translation_replacements.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/apply_translation_replacements.py)
- [android_string_text.py](/C:/code/WaveBits/tools/scripts/android/translate/core/android_string_text.py)

### 4. 想改 JSON 自动修复能力

去：

- [replacement_json_preflight.py](/C:/code/WaveBits/tools/scripts/android/translate/core/replacement_json_preflight.py)

### 5. 想改“哪些语言生成 review”

去：

- [compare_translation_quality.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/compare_translation_quality.py)

### 6. 想改 sample text 的 faction 分组

去：

- [translation_paths.py](/C:/code/WaveBits/tools/scripts/android/translate/core/translation_paths.py)

### 7. 想改 key alignment / mixed-language 检查规则

去：

- [check_translation_key_alignment.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/check_translation_key_alignment.py)
- [check_mixed_language.py](/C:/code/WaveBits/tools/scripts/android/translate/commands/check_mixed_language.py)

## 文档索引

- [sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)
- [compare_translation_quality.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/compare_translation_quality.md)
- [apply_translation_replacements.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/apply_translation_replacements.md)
- [check_mixed_language.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_mixed_language.md)
- [check_translation_key_alignment.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_translation_key_alignment.md)
