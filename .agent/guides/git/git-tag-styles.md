---
description: Agent 专用 Git 标签模板
---

# Git Tag Template

本文件只定义 agent 创建、重挂或修正 Git tag 时必须遵守的最小规则与模板。

## Hard Rules

- 正式发布标签必须使用 `vX.Y.Z`
- 禁止使用表情、空格或自定义装饰后缀
- 正式发布默认使用 annotated tag
- tag 版本必须与目标发布版本一致
- 若历史被改写，需检查并按需要重挂 tag
- 错误 tag 不保留，以规范 tag 为准
- 涉及中文 tag 注释消息的生成、落盘或 `-F/--file` 提交时，优先使用 `pwsh` / `pwsh.exe`
- 需要将 tag 注释写入文件时，使用 `pwsh` 的 UTF-8 输出（如 `Set-Content -Encoding utf8`），避免中文乱码

## Tag Name Template

```text
vX.Y.Z
```

## Annotated Message Template

最小形式：

```text
vX.Y.Z
```

带正文形式：

```text
vX.Y.Z

Release for vX.Y.Z.
```

## Command Template

创建：

```bash
git tag -a vX.Y.Z -m "vX.Y.Z"
git push origin vX.Y.Z
```

重挂：

```bash
git tag -d vX.Y.Z
git push origin :refs/tags/vX.Y.Z
git tag -a vX.Y.Z <commit> -m "vX.Y.Z"
git push origin vX.Y.Z
```

## Consistency Rules

- 代码中的版本号应与正式 tag 一致
- 对应发布说明文件应存在
- 若用户明确要求重挂到指定提交或当前 `HEAD`，agent 直接执行，不重复询问
