---
description: 
---

# 程序文件命名规范（高置信度：小文件但不合并）

## 1. 目标
- 明确哪些“小文件”属于契约/数据载体，应该保持独立，不因行数少被合并。
- 通过文件名即可表达职责，降低评审歧义与错误合并概率。

## 2. 基础规则
- 使用 `snake_case`，全小写英文。
- 文件名优先用“业务域 + 角色”结构：`<domain>_<role>.<ext>`。
- 禁止含糊命名：`utils.hpp`、`common.hpp`、`temp.hpp`、`misc.hpp`。

## 3. 高置信度不可合并文件（命名规则）

### 3.1 契约接口（Interface Contract）
- 规则：`i_<capability>.hpp`
- 约束：仅声明接口，不写业务实现。
- 示例：`i_report_formatter.hpp`、`i_data_query_service.hpp`

### 3.2 数据载体（Data Carrier）
- DTO：`<domain>_dto.hpp` 或 `<domain>_requests.hpp` / `<domain>_responses.hpp`
- 模型：`<domain>_model(s).hpp`
- 类型集合：`<domain>_types.hpp`
- 配置载体：`<domain>_config.hpp`
- 约束：只放字段、轻量校验、无流程逻辑。
- 示例：`core_requests.hpp`、`project_tree_data.hpp`、`report_types.hpp`

### 3.3 契约元信息（Schema / Manifest / Registry / Aliases）
- `*_schema.*`：结构/字段约定（如 SQL、JSON）
- `*_manifest.*`：插件或模块清单
- `*_registry.*`：注册入口与映射注册
- `*_aliases.*`：别名常量
- 示例：`sqlite_schema.hpp`、`plugin_manifest.hpp`、`formatter_registry.hpp`、`sql_aliases.hpp`

## 4. 何时允许存在 `.cpp`
- 默认契约/载体文件只需 `.hpp`。
- 仅当出现“注册动作、工厂组装、静态映射初始化”时，允许同名 `.cpp`：
  - `<name>.hpp` + `<name>.cpp`
  - 例如：`formatter_registry.hpp/.cpp`

## 5. 何时判定为“不应合并”
- 文件主要内容是：
  - 接口声明（纯虚类、函数签名）
  - 常量协议（schema/aliases）
  - DTO/模型字段定义
  - 注册入口声明
- 且不包含复杂流程控制、I/O、数据库读写、业务分支编排。

## 6. 推荐与反例
- 推荐：
  - `sqlite_schema.hpp`
  - `plugin_manifest.hpp`
  - `formatter_registry.hpp`
  - `report_types.hpp`
  - `runtime_environment_requirements.hpp`
- 反例：
  - `schema_utils.hpp`（角色不清）
  - `all_contracts.hpp`（职责聚合过度）
  - `registry_and_manifest.hpp`（多职责混合）
