---
description: Update Markdown Description
---

### Update Markdown Description
#### 1. 结构与格式 (Structure & Formatting)
- **逆序排列**: 最新的版本必须出现在文件顶部（在 Header 之下）。
- **版本标题**: 必须使用二级标题，格式为 `## [vX.Y.Z] - YYYY-MM-DD`。
- **日期标准**: 必须遵循 ISO 8601 标准 (`YYYY-MM-DD`)。
- **空行**: 每个版本块之间保留两个空行；版本标题与第一个分类标题之间保留一个空行。

#### 2. 分类准则 (Classification Rules)
不要使用随意的标题，必须将改动归类为以下标准 KAC 类别（括号内为你项目特定的映射）：
- **新增功能 (Added)**: 用于新发布的功能（例如：新增 `export quarter` 命令）。
- **技术改进/重构 (Changed/Refactor)**: 用于现有功能的变更、架构调整或大规模重构（例如：`snake_case` 转 `PascalCase`）。
- **修复 (Fixed)**: 用于任何 Bug 的修复。
- **安全性 (Security)**: 用于修复安全漏洞（如有）。
- **弃用/删除 (Deprecated/Removed)**: 用于标记即将移除或已移除的内容。

#### 3. 内容编写习惯 (Writing Style)
- **条目化**: 使用无序列表 `*`。
- **重点加粗**: 对模块名、核心类名或关键配置项进行加粗（例如：`**RangeReportService**`）。
- **代码高亮**: 所有的 CLI 命令、方法名、文件名、配置键名必须使用反引号包裹（例如：`export all-week`）。
- **简洁有力**: 以动词开头（如：新增、重构、修复、优化、同步）。

#### 4. 项目特定约束 (Project Constraints)
- **禁止 `-ly`**: 在记录命令相关内容时，确保使用的是名词形式（`day`, `week`, `month`, `year`），禁止写成 `daily`, `weekly` 等。
- **命名同步**: 如果记录涉及代码重构，确保体现了从 `snake_case` 到 `PascalCase` 的转变（例如：`read_content` → `ReadContent`）。
- **路径记录**: 涉及目录变更时，需指明具体路径（如：`src/application/pipeline/`）。

#### 5. 自动维护链接 (Optional but Recommended)
- 如果存在版本对比，在文件末尾维护引用链接：
  `[v0.5.5]: https://github.com/username/project/compare/v0.5.4...v0.5.5`