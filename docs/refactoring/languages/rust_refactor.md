# Rust Refactor Goal

仓库职责定位：Rust 当前主要服务于 `apps/audio_cli` 的命令、presentation 与 native FFI adapter。

优先判断：

- command orchestration、filesystem IO、terminal output 是否混入 ABI/metadata 规则
- unsafe/raw pointer 的创建、poll、take、free/drop 是否由同一个 guard owner 管理
- 子模块是否使用 `use super::*` 隐藏依赖
- 是否为了降低行数拆出只有少量转发函数的 `raw/status/output` 微模块

重构原则：

- ABI declarations、ownership guard、domain conversion、command orchestration、presentation 可以形成边界，但每个边界必须有独立修改理由
- create/poll/take/drop 等同一资源生命周期保持高内聚
- 使用显式 import，避免通过父模块 wildcard 获得隐式依赖
- 小型共享转换只有被多个 owner 使用时才独立成模块，否则留在其唯一 owner 内
- 保持 stdout/stderr、exit code、C ABI 和资源释放语义不变

验证：

```powershell
python tools/run.py cli test
```
