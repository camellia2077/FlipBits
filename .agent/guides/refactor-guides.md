---
description: Refactor Guides Description
---
# 高内聚、低耦合、减少碎片化重构指南

## 目标

重构不是“减少文件数”，而是让代码按职责组织：

- 高内聚：同一业务变化点尽量落在同一模块。
- 低耦合：跨层、跨模块依赖最小化，依赖方向稳定。
- 可维护：读一个功能时，跳转文件数量可控，变更影响面可预测。

## 什么应该合并

满足以下条件时，优先合并：

- 同一变化轴：文件总是一起改动，且共同服务一个明确能力。
- 同一使用域：只在同一子模块内部使用，没有独立复用价值。
- 语义强绑定：拆开后需要来回跳转才能理解完整流程。
- 非契约文件：主要是实现细节（`*.cpp`、仅内部使用的 helper）。

典型可合并对象：

- 同维度查询器与其批量抓取实现（如 `daily_querier + batch_daily_fetcher`）。
- 同一格式器下的超小配置实现文件（`*_config.cpp` 并入 `*_formatter.cpp`）。
- 同类策略碎片（如 `daily_md/daily_tex/daily_typ` 收敛到 `daily_strategies.cpp`）。
- 重复测试装配代码（如多个测试模块重复的 fixture/build helper）。

## 什么不应该合并

以下文件默认保持独立，不为“减少文件数”而合并：

- 分层契约：`ports/`、`interfaces/`、`dto/`、`abi/`。
- 跨模块复用的公共类型与协议定义。
- 公开 API 边界（被多目标、多层依赖的稳定头文件）。
- 语义不同、发布节奏不同、测试策略不同的模块。

反例：

- 把多个 `I*` 接口并成一个“大接口头”。
- 把多个 DTO 混成“万能 models.hpp”。
- 把 domain 规则与 infrastructure 适配实现混在一个文件。

## 快速判定规则

合并前用四问法，全部回答“是”才合并：

1. 这两个文件是否总是一起改？
2. 它们是否只服务同一个子能力？
3. 合并后是否不会引入新的跨层依赖？
4. 合并后是否能减少理解路径而不损伤复用边界？

任意一项为“否”，先不要合并。

## 推荐流程

1. 先定边界：domain/application/infrastructure/api 的依赖方向不变。
2. 先收敛实现碎片：从 `5~30` 行、仅内部使用的实现文件开始。
3. 再收敛同类策略/同维度模块：按 daily/monthly/weekly/yearly 等轴聚合。
4. 最后清理兼容层：删除不再需要的 forwarding header/旧文件。
5. 每批次必须验证：build、核心测试、suite、层级检查全通过。

## 验证标准（必须）

- `python scripts/run.py build --app tracer_core --build-dir build_fast` 通过。
- `ctest --test-dir apps/tracer_core/build_fast -R "^time_tracker_core_api_tests$"` 通过。
- `python test/run.py --suite time_tracer --agent --build-dir build_fast --concise` 通过。

## 评估指标=

- 一个功能主流程的“阅读跳转文件数”下降。
- 一次需求改动触达文件数下降。
- 无新增层级违规、无新增循环依赖。
- 测试可读性提升，重复装配逻辑减少。
