# Responsibility Risk Rules

`tools/scripts/loc` 里的职责混杂扫描目前是“启发式预警”，不是 AST 级设计审查。
它的目标是尽快把值得人工复查的文件筛出来，而不是替代架构判断。

## Kotlin 当前规则

当前 Kotlin 扫描关注 5 组信号：

- 行数
  - 超过 `responsibility_line_threshold` 加 2 分
  - 超过 `responsibility_line_threshold + 120` 再加 1 分
- 状态信号
  - 统计 `remember*`、`mutableStateOf`、`LaunchedEffect`
  - 命中数达到 `responsibility_state_signal_threshold` 开始加分
  - 再按 `+2`、`+5` 的阶梯继续加分
- 顶层 `@Composable`
  - 只统计顶层 `@Composable fun`
  - 数量达到 `responsibility_top_level_composable_threshold` 开始加分
  - 再按 `+2`、`+4` 的阶梯继续加分
- 命名角色种类
  - 统计顶层 composable 名字里是否出现：
    - `Section`
    - `Block`
    - `Card`
    - `Switcher`
    - `Timeline`
  - 命中的角色种类数达到 `responsibility_role_kind_threshold` 后开始加分
- 模式分支
  - 统计 `if/when` 中和 `Mode`、`selected*`、`viewMode`、`displayMode` 相关的分支
  - 命中数达到 `responsibility_mode_branch_threshold` 后开始加分

当前优先级分档：

- `score >= 9` -> `P0`
- `score >= 7` -> `P1`
- `score >= 5` -> `P2`
- 其他 -> `P3`

Kotlin 的设计意图：

- 把“页面入口 + 多个 section/block + 多状态 + 多模式切换”这类文件优先打出来
- 不试图判断 UI 设计本身对不对
- 更偏向“找出值得拆分讨论的文件”

## Python 第一版规则

Python 第一版也保持保守，只做“脚本/模块级职责混杂预警”。
它同样使用 5 组信号，但信号内容换成 Python 语境。

### 1. 行数

- 超过 `responsibility_line_threshold` 加 2 分
- 超过 `responsibility_line_threshold + 120` 再加 1 分

建议默认阈值：

- `responsibility_line_threshold = 220`

### 2. 状态与副作用信号

统计这些模式的命中次数：

- `self.`
- `global`
- `nonlocal`
- `os.environ`
- `threading`
- `asyncio`
- `subprocess`
- `requests`

设计意图：

- 不是说这些模式本身不好
- 而是“文件已经偏大时，又同时持有状态、环境依赖和副作用”更值得复查

建议默认阈值：

- `responsibility_state_signal_threshold = 4`

### 3. 顶层符号数量

统计顶层：

- `def`
- `async def`
- `class`

设计意图：

- 如果一个 Python 文件里堆了很多顶层定义，它更容易同时承担解析、协调、IO、格式化等多种职责

建议默认阈值：

- `responsibility_top_level_composable_threshold = 6`

说明：

- 这里沿用了现有配置字段名，虽然名字里还是 `composable`
- 在 Python 语境里，它实际代表“顶层符号数”

### 4. 命名角色种类

统计顶层符号名里是否出现这些角色词：

- `Manager`
- `Service`
- `Controller`
- `Handler`
- `Client`
- `Builder`
- `Parser`
- `Formatter`
- `Loader`
- `Writer`

设计意图：

- 一个文件里如果同时出现很多不同角色名，通常是在混搭多类职责

建议默认阈值：

- `responsibility_role_kind_threshold = 2`

### 5. 模式分支

统计这些模式分支信号：

- `if ... mode ==`
- `if ... kind ==`
- `if ... type ==`
- `elif ... mode ==`
- `elif ... kind ==`
- `elif ... type ==`
- `match`
- `case`

设计意图：

- 一个文件里如果有很多“按 mode/kind/type 切多路逻辑”的代码，往往意味着多个执行路径被揉在同一个模块里

建议默认阈值：

- `responsibility_mode_branch_threshold = 2`

## 当前边界

当前职责混杂扫描的边界是：

- 只做文件级预警
- 不做 AST 级数据流分析
- 不判断函数是否纯函数
- 不直接给出“必须拆分”的结论
- 输出主要用于：
  - 发现可疑大文件
  - 排序拆分优先级
  - 作为后续人工 review 的入口
