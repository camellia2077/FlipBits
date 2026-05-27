# Android Custom Theme Import Format

## Goal

这份文档说明 Android `Settings` 里自定义颜色的导入/导出文本格式，覆盖：

- `Material custom`
- `dual-tone custom`
- 内置 `dual-tone` 的复制配置文本

目标是同时满足：

- 手机与电脑都易读
- 用户方便复制、分享、粘贴
- 新格式尽量节省行数
- 继续兼容历史导出文本

## Scope

这里的规则适用于：

- 导出自定义 `Material` 颜色
- 导出自定义 `dual-tone` 颜色
- 导出内置 `dual-tone` 颜色
- 从剪贴板导入上述颜色文本

不适用于：

- 主题 UI 展示规则
- dual-tone 视觉语义
- `BrandThemeCatalog.kt` 的主题配色职责

这些内容继续看：

- [android-dual-tone-theme.md](./android-dual-tone-theme.md)

## Current Export Format

### Material

每组颜色 1 行，组与组之间空 1 行。

字段顺序固定：

- `name`
- `primary`

示例：

```text
name=Paper primary=#E5E9F0

name=Night primary=#111827
```

### Dual-tone

每组颜色 1 行，组与组之间空 1 行。

字段顺序固定：

- `name`
- `primary`
- `secondary`
- `outline`

示例：

```text
name=Ash primary=#101014 secondary=#78D6FF outline=#303846

name=Brass primary=#E8E2D0 secondary=#9E1B1B outline=#C78C25
```

## Allowed Keys

### Material

只允许：

- `name`
- `primary`

### Dual-tone

只允许：

- `name`
- `primary`
- `secondary`
- `outline`

key 必须按上面名称拼写。

## Hex Rules

- `primary` / `secondary` / `outline` 必须是 6 位十六进制 RGB
- 允许带 `#`
- 也允许不带 `#`
- 解析后统一归一化成大写 `#RRGGBB`

有效示例：

- `#E5E9F0`
- `E5E9F0`
- `#1a100c`

无效示例：

- `#FFF`
- `ZZZZZZ`
- `#12GG34`

## Import Compatibility

导入必须兼容下面几类文本。

### 1. Current Single-line Format

字段之间用空格分隔：

```text
name=Ash primary=#101014 secondary=#78D6FF outline=#303846
```

### 2. Compact Single-line Format

即使没有空格、没有换行，只要后面仍然出现合法 key，也要能切开：

```text
name=Ashprimary=#101014secondary=#78D6FFoutline=#303846
```

```text
name=Paperprimary=#E5E9F0
```

这个放宽能力建立在下面条件上：

- key 名必须正确
- key 必须是合法字段名
- hex 必须仍然是合法 6 位 RGB

如果 key 拼错，解析器不应该猜测用户本意。

### 3. Legacy Multi-line Material Format

历史两行块格式继续兼容：

```text
name=Paper
primary=#E5E9F0
```

### 4. Legacy Multi-line Dual-tone Format

历史四行块格式继续兼容：

```text
name=Ash
primary=#101014
secondary=#78D6FF
outline=#303846
```

## Parsing Rules

### Group Boundary

当前导入支持两类分组方式：

- 多行块格式下，空行或下一个 `name=...` 开始新组
- 单行格式下，每一行视为 1 组

### Compact Parsing

当用户粘贴没有空格、没有换行的文本时，解析器只能依赖合法 key 作为切分锚点：

- `name=`
- `primary=`
- `secondary=`
- `outline=`

所以：

- `name=火primary=#1E7A3Esecondary=#FF6B00outline=#111318`
  - 可以解析
- `name=火primry=#1E7A3E...`
  - 不应该自动猜成 `primary`

## Error Handling

手机场景下，错误提示不应该强调“第几行”，因为用户看到的换行常常只是记事本或输入框的视觉换行，不等于真实配置边界。

导入失败时，提示应优先按“组”来报：

- `第 3 组（Ash）缺少 secondary`
- `第 2 组（Paper）primary 颜色无效：#ZZZZZZ`
- `第 4 组（未命名）包含未知字段：tertiary`
- `第 1 组（Keyboard）看起来是 dual-tone，请从 dual-tone 导入`

推荐规则：

- 如果已成功解析出 `name`，提示里带名字
- 如果 `name` 还没拿到，就退回 `第 N 组`
- `MalformedLine` 这类报错只作为底层兜底，不应成为手机 UI 的首选提示语言

## Duplicate Matching

导入后的重复判定规则：

### Material

只有下面两项都相同，才算重复：

- `name`
- `primary`

### Dual-tone

只有下面四项都相同，才算重复：

- `name`
- `primary`
- `secondary`
- `outline`

## Maintenance Notes

颜色导入/导出规则已经不再是“简单 key-value 文本”，后续修改时不要继续把职责堆进一个大函数。

建议至少按下面 3 层维护：

1. Export formatting
   - 决定写成几行
   - 决定字段顺序
   - 决定组间分隔

2. Import tokenization
   - 识别 key
   - 兼容单行 / 紧贴 / 多行块
   - 只负责把原始文本拆成字段

3. Import validation
   - 缺字段
   - 非法 hex
   - 错误导入模式
   - 重复字段
   - 未知字段

如果未来继续扩展格式：

- 优先增加测试，再改解析器
- 明确区分“为了兼容旧文本”的规则与“新的标准导出格式”
