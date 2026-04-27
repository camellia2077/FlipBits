from __future__ import annotations

from enum import StrEnum


class ResponsibilityRiskKind(StrEnum):
    COMMAND_LAYER_LEAK = "command_layer_leak"
    INTEROP_SURFACE_BREADTH = "interop_surface_breadth"
    IO_SURFACE_BREADTH = "io_surface_breadth"
    MIXED_RESPONSIBILITY_VERBS = "mixed_responsibility_verbs"
    MODE_BRANCHING = "mode_branching"
    RESOURCE_LIFECYCLE_DENSITY = "resource_lifecycle_density"
    RULE_HELPER_DENSITY = "rule_helper_density"
    STATEFUL_SHARED_RESOURCES = "stateful_shared_resources"
    STATEFUL_SIDE_EFFECTS = "stateful_side_effects"


RISK_SUMMARY_TEXT = {
    ResponsibilityRiskKind.COMMAND_LAYER_LEAK: "commands 层混入底层规则 helper",
    ResponsibilityRiskKind.INTEROP_SURFACE_BREADTH: "同一文件同时覆盖多层桥接/FFI 表面",
    ResponsibilityRiskKind.IO_SURFACE_BREADTH: "同一模块覆盖多种 IO / 平台接口",
    ResponsibilityRiskKind.MIXED_RESPONSIBILITY_VERBS: "同时承担读取、校验、定位、变更、桥接或销毁等多类动词簇",
    ResponsibilityRiskKind.MODE_BRANCHING: "mode/style/state 分支较多",
    ResponsibilityRiskKind.RESOURCE_LIFECYCLE_DENSITY: "资源申请、释放和生命周期管理信号偏密",
    ResponsibilityRiskKind.RULE_HELPER_DENSITY: "规则 helper、转换 helper 和小工具函数过密",
    ResponsibilityRiskKind.STATEFUL_SHARED_RESOURCES: "共享状态/线程原语偏多",
    ResponsibilityRiskKind.STATEFUL_SIDE_EFFECTS: "状态/副作用信号偏多",
}
