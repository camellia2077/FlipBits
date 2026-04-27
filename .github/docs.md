# GitHub Actions

`.github/` 只放仓库自动化入口与 workflow 配置。

## 当前入口

- `workflows/ci-host-verify.yml`
  - Host 默认主线 CI
- `workflows/ci-android-assemble.yml`
  - Android Debug 组装 CI
- `workflows/ci-android-quality.yml`
  - Android Kotlin 质量 CI

## 说明

- 当前 CI 职责地图、触发范围与修改口径统一看：
  - `C:\code\WaveBits\docs\tools\ci-workflow.md`
