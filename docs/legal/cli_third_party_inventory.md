# FlipBits CLI Third-Party Inventory

更新时间：2026-04-21

## 目的

这份文档是 `FlipBits` CLI 的第三方依赖稳定来源文件，只覆盖：

- Rust CLI 可执行程序
- Rust CLI 的构建期依赖
- Rust CLI 的测试期依赖

这份文档不覆盖 Android，也不覆盖整个仓库的总览角色。仓库级总览仍保留在 `docs/legal/third_party_inventory.md`。

当前也**不包含 `libsndfile`**。原因是 native 构建链虽然存在 `sndfile` 依赖线索，但截至 2026-04-21，已验证的 Windows CLI 主功能路径在运行时不要求 `libsndfile-1.dll`，所以第一版 CLI notices 不先把它写死。

## 分发期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| clap | 4.6.1 | `apps/audio_cli/rust/Cargo.toml` / `Cargo.lock` | Rust CLI 参数解析 | 已确认 | 当前唯一直接运行时 crates.io 依赖 |
| clap_builder | 4.6.0 | `cargo tree --edges normal,build` | `clap` 运行时传递依赖 | 已确认 | 由 `clap` 引入 |
| anstream | 1.0.0 | `cargo tree --edges normal,build` | 终端样式输出支持 | 已确认 | 由 `clap_builder` 引入 |
| anstyle | 1.0.14 | `cargo tree --edges normal,build` | ANSI 样式支持 | 已确认 | `clap` 依赖链的一部分 |
| anstyle-parse | 1.0.0 | `cargo tree --edges normal,build` | ANSI 样式解析 | 已确认 | `clap` 依赖链的一部分 |
| anstyle-query | 1.1.5 | `cargo tree --edges normal,build` | 终端能力查询 | 已确认 | Windows 终端能力相关 |
| anstyle-wincon | 3.0.11 | `cargo tree --edges normal,build` | Windows 控制台样式支持 | 已确认 | Windows 相关传递依赖 |
| colorchoice | 1.0.5 | `cargo tree --edges normal,build` | 颜色输出策略 | 已确认 | `clap` 依赖链的一部分 |
| is_terminal_polyfill | 1.70.2 | `cargo tree --edges normal,build` | 终端检测兼容层 | 已确认 | `clap` 依赖链的一部分 |
| utf8parse | 0.2.2 | `cargo tree --edges normal,build` | UTF-8 解析支持 | 已确认 | `anstream` / `anstyle-parse` 传递依赖 |
| clap_lex | 1.1.0 | `cargo tree --edges normal,build` | 参数词法拆分 | 已确认 | `clap_builder` 传递依赖 |
| strsim | 0.11.1 | `cargo tree --edges normal,build` | 命令建议相似度匹配 | 已确认 | `clap_builder` 传递依赖 |
| windows-sys | 0.61.2 | `cargo tree --edges normal,build` | Windows 系统 API 绑定 | 已确认 | 当前通过 `anstyle-query` / `anstyle-wincon` 引入 |
| windows-link | 0.2.1 | `cargo tree --edges normal,build` | Windows 链接辅助 | 已确认 | 当前通过 `windows-sys` 引入 |

## 构建期依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| clap_derive | 4.6.1 | `cargo tree --edges normal,build` | `clap` proc-macro | 已确认 | 构建期代码生成 |
| heck | 0.5.0 | `cargo tree --edges normal,build` | 标识符大小写转换 | 已确认 | `clap_derive` 传递依赖 |
| proc-macro2 | 1.0.106 | `cargo tree --edges normal,build` | proc-macro 基础设施 | 已确认 | `clap_derive` 传递依赖 |
| quote | 1.0.45 | `cargo tree --edges normal,build` | proc-macro 代码输出 | 已确认 | `clap_derive` 传递依赖 |
| syn | 2.0.117 | `cargo tree --edges normal,build` | Rust 语法解析 | 已确认 | `clap_derive` 传递依赖 |
| unicode-ident | 1.0.24 | `cargo tree --edges normal,build` | Unicode 标识符支持 | 已确认 | proc-macro 依赖链的一部分 |

## 开发/测试依赖

| Name | Version | 来源 | 用途 | 状态 | 备注 |
| --- | --- | --- | --- | --- | --- |
| assert_cmd | 2.2.1 | `Cargo.lock` | CLI 集成测试命令断言 | 已确认 | `dev-dependencies` |
| predicates | 3.1.4 | `Cargo.lock` | CLI 测试断言 | 已确认 | `dev-dependencies` |
| tempfile | 3.27.0 | `Cargo.lock` | CLI 测试临时目录 | 已确认 | `dev-dependencies` |

## 与 notices 的关系

- `docs/legal/cli_third_party_notices.md` 是 `FlipBits licenses` 第一版面向分发的 notices 来源
- 本文件更偏向“依赖来源盘点”
- notices 文件会在此基础上补许可证标识、上游主页和许可证文本链接

## 运行时验证备注

- native 构建链中，`build/dev/build.ninja` 当前会把 `sndfile` 解析到 `C:/msys64/ucrt64/lib/libsndfile.dll.a`
- 但在 2026-04-21 的本地运行时隔离验证中，只携带 `FlipBits.exe`、`libstdc++-6.dll`、`libwinpthread-1.dll` 和 `libgcc_s_seh-1.dll`，不携带 `libsndfile-1.dll`
- 在该条件下，`FlipBits version`、`FlipBits licenses`、`FlipBits encode`、`FlipBits decode` 均验证通过
- 因此，截至本次验证，`libsndfile` 仍不作为已确认的 CLI runtime 分发依赖写入本文件
