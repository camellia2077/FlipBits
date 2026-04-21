# FlipBits CLI Third-Party Notices

更新时间：2026-04-21

## 使用范围

本文件只适用于 `FlipBits` CLI。

- 仅覆盖 CLI
- 不包含 Android
- 当前不包含 `libsndfile`

当前 `libsndfile` 未纳入的原因是：native 构建链虽然存在 `sndfile` 依赖线索，但截至 2026-04-21，已验证的 Windows CLI 主功能路径在运行时不要求 `libsndfile-1.dll`。因此第一版 notices 先不对 native 依赖做错误承诺。

## 汇总表

| Name | Version | Category | License | Source | Notes |
| --- | --- | --- | --- | --- | --- |
| clap | 4.6.1 | runtime | MIT OR Apache-2.0 | crates.io / `Cargo.lock` | CLI 直接依赖 |
| clap_builder | 4.6.0 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | `clap` 运行时传递依赖 |
| anstream | 1.0.0 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | `clap_builder` 运行时传递依赖 |
| anstyle | 1.0.14 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | ANSI 样式支持 |
| anstyle-parse | 1.0.0 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | ANSI 样式解析 |
| anstyle-query | 1.1.5 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | Windows 终端能力查询 |
| anstyle-wincon | 3.0.11 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | Windows 控制台样式支持 |
| colorchoice | 1.0.5 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | 颜色输出策略 |
| is_terminal_polyfill | 1.70.2 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | 终端检测兼容层 |
| utf8parse | 0.2.2 | runtime | Apache-2.0 OR MIT | crates.io / `cargo tree` | UTF-8 解析支持 |
| clap_lex | 1.1.0 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | 参数词法拆分 |
| strsim | 0.11.1 | runtime | MIT | crates.io / `cargo tree` | 相似命令建议 |
| windows-sys | 0.61.2 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | Windows 系统 API 绑定 |
| windows-link | 0.2.1 | runtime | MIT OR Apache-2.0 | crates.io / `cargo tree` | Windows 链接辅助 |
| clap_derive | 4.6.1 | build | MIT OR Apache-2.0 | crates.io / `cargo tree` | `clap` proc-macro |
| heck | 0.5.0 | build | MIT OR Apache-2.0 | crates.io / `cargo tree` | 标识符大小写转换 |
| proc-macro2 | 1.0.106 | build | MIT OR Apache-2.0 | crates.io / `cargo tree` | proc-macro 基础设施 |
| quote | 1.0.45 | build | MIT OR Apache-2.0 | crates.io / `cargo tree` | proc-macro 代码输出 |
| syn | 2.0.117 | build | MIT OR Apache-2.0 | crates.io / `cargo tree` | Rust 语法解析 |
| unicode-ident | 1.0.24 | build | (MIT OR Apache-2.0) AND Unicode-3.0 | crates.io / `cargo tree` | Unicode 标识符支持 |
| assert_cmd | 2.2.1 | test | MIT OR Apache-2.0 | crates.io / `Cargo.lock` | CLI 集成测试命令断言 |
| predicates | 3.1.4 | test | MIT OR Apache-2.0 | crates.io / `Cargo.lock` | CLI 测试断言 |
| tempfile | 3.27.0 | test | MIT OR Apache-2.0 | crates.io / `Cargo.lock` | CLI 测试临时目录 |

## Notices

### clap 4.6.1

License: MIT OR Apache-2.0  
Project: <https://github.com/clap-rs/clap>  
License text: <https://docs.rs/crate/clap/4.6.1/source/LICENSE-APACHE> and <https://docs.rs/crate/clap/4.6.1/source/LICENSE-MIT>

### clap_builder 4.6.0

License: MIT OR Apache-2.0  
Project: <https://github.com/clap-rs/clap>  
License text: <https://docs.rs/crate/clap_builder/4.6.0/source/LICENSE-APACHE> and <https://docs.rs/crate/clap_builder/4.6.0/source/LICENSE-MIT>

### anstream 1.0.0

License: MIT OR Apache-2.0  
Project: <https://github.com/rust-cli/anstyle.git>  
License text: <https://docs.rs/crate/anstream/1.0.0/source/LICENSE-APACHE> and <https://docs.rs/crate/anstream/1.0.0/source/LICENSE-MIT>

### anstyle 1.0.14

License: MIT OR Apache-2.0  
Project: <https://github.com/rust-cli/anstyle.git>  
License text: <https://docs.rs/crate/anstyle/1.0.14/source/LICENSE-APACHE> and <https://docs.rs/crate/anstyle/1.0.14/source/LICENSE-MIT>

### anstyle-parse 1.0.0

License: MIT OR Apache-2.0  
Project: <https://github.com/rust-cli/anstyle.git>  
License text: <https://docs.rs/crate/anstyle-parse/1.0.0/source/LICENSE-APACHE> and <https://docs.rs/crate/anstyle-parse/1.0.0/source/LICENSE-MIT>

### anstyle-query 1.1.5

License: MIT OR Apache-2.0  
Project: <https://github.com/rust-cli/anstyle.git>  
License text: <https://docs.rs/crate/anstyle-query/1.1.5/source/LICENSE-APACHE> and <https://docs.rs/crate/anstyle-query/1.1.5/source/LICENSE-MIT>

### anstyle-wincon 3.0.11

License: MIT OR Apache-2.0  
Project: <https://github.com/rust-cli/anstyle.git>  
License text: <https://docs.rs/crate/anstyle-wincon/3.0.11/source/LICENSE-APACHE> and <https://docs.rs/crate/anstyle-wincon/3.0.11/source/LICENSE-MIT>

### colorchoice 1.0.5

License: MIT OR Apache-2.0  
Project: <https://github.com/rust-cli/anstyle.git>  
License text: <https://docs.rs/crate/colorchoice/1.0.5/source/LICENSE-APACHE> and <https://docs.rs/crate/colorchoice/1.0.5/source/LICENSE-MIT>

### is_terminal_polyfill 1.70.2

License: MIT OR Apache-2.0  
Project: <https://github.com/polyfill-rs/is_terminal_polyfill>  
License text: <https://docs.rs/crate/is_terminal_polyfill/1.70.2/source/LICENSE-APACHE> and <https://docs.rs/crate/is_terminal_polyfill/1.70.2/source/LICENSE-MIT>

### utf8parse 0.2.2

License: Apache-2.0 OR MIT  
Project: <https://github.com/alacritty/vte>  
License text: <https://docs.rs/crate/utf8parse/0.2.2/source/LICENSE-APACHE> and <https://docs.rs/crate/utf8parse/0.2.2/source/LICENSE-MIT>

### clap_lex 1.1.0

License: MIT OR Apache-2.0  
Project: <https://github.com/clap-rs/clap>  
License text: <https://docs.rs/crate/clap_lex/1.1.0/source/LICENSE-APACHE> and <https://docs.rs/crate/clap_lex/1.1.0/source/LICENSE-MIT>

### strsim 0.11.1

License: MIT  
Project: <https://github.com/rapidfuzz/strsim-rs>  
License text: <https://docs.rs/crate/strsim/0.11.1/source/LICENSE>

### windows-sys 0.61.2

License: MIT OR Apache-2.0  
Project: <https://github.com/microsoft/windows-rs>  
License text: <https://docs.rs/crate/windows-sys/0.61.2/source/license-apache-2.0> and <https://docs.rs/crate/windows-sys/0.61.2/source/license-mit>

### windows-link 0.2.1

License: MIT OR Apache-2.0  
Project: <https://github.com/microsoft/windows-rs>  
License text: <https://docs.rs/crate/windows-link/0.2.1/source/license-apache-2.0> and <https://docs.rs/crate/windows-link/0.2.1/source/license-mit>

### clap_derive 4.6.1

License: MIT OR Apache-2.0  
Project: <https://github.com/clap-rs/clap>  
License text: <https://docs.rs/crate/clap_derive/4.6.1/source/LICENSE-APACHE> and <https://docs.rs/crate/clap_derive/4.6.1/source/LICENSE-MIT>

### heck 0.5.0

License: MIT OR Apache-2.0  
Project: <https://github.com/withoutboats/heck>  
License text: <https://docs.rs/crate/heck/0.5.0/source/LICENSE-APACHE> and <https://docs.rs/crate/heck/0.5.0/source/LICENSE-MIT>

### proc-macro2 1.0.106

License: MIT OR Apache-2.0  
Project: <https://github.com/dtolnay/proc-macro2>  
License text: <https://docs.rs/crate/proc-macro2/1.0.106/source/LICENSE-APACHE> and <https://docs.rs/crate/proc-macro2/1.0.106/source/LICENSE-MIT>

### quote 1.0.45

License: MIT OR Apache-2.0  
Project: <https://github.com/dtolnay/quote>  
License text: <https://docs.rs/crate/quote/1.0.45/source/LICENSE-APACHE> and <https://docs.rs/crate/quote/1.0.45/source/LICENSE-MIT>

### syn 2.0.117

License: MIT OR Apache-2.0  
Project: <https://github.com/dtolnay/syn>  
License text: <https://docs.rs/crate/syn/2.0.117/source/LICENSE-APACHE> and <https://docs.rs/crate/syn/2.0.117/source/LICENSE-MIT>

### unicode-ident 1.0.24

License: (MIT OR Apache-2.0) AND Unicode-3.0  
Project: <https://github.com/dtolnay/unicode-ident>  
License text: <https://docs.rs/crate/unicode-ident/1.0.24/source/LICENSE-APACHE>, <https://docs.rs/crate/unicode-ident/1.0.24/source/LICENSE-MIT>, and <https://docs.rs/crate/unicode-ident/1.0.24/source/LICENSE-UNICODE>

### assert_cmd 2.2.1

License: MIT OR Apache-2.0  
Project: <https://github.com/assert-rs/assert_cmd.git>  
License text: <https://docs.rs/crate/assert_cmd/2.2.1/source/LICENSE-APACHE> and <https://docs.rs/crate/assert_cmd/2.2.1/source/LICENSE-MIT>

### predicates 3.1.4

License: MIT OR Apache-2.0  
Project: <https://github.com/assert-rs/predicates-rs>  
License text: <https://docs.rs/crate/predicates/3.1.4/source/LICENSE-APACHE> and <https://docs.rs/crate/predicates/3.1.4/source/LICENSE-MIT>

### tempfile 3.27.0

License: MIT OR Apache-2.0  
Project: <https://github.com/Stebalien/tempfile>  
License text: <https://docs.rs/crate/tempfile/3.27.0/source/LICENSE-APACHE> and <https://docs.rs/crate/tempfile/3.27.0/source/LICENSE-MIT>

## 仓库说明

- 本文件是 `FlipBits licenses` 第一版 notices 稳定来源
- `FlipBits licenses` 当前只输出摘要和本文件路径，不直接输出全文
- 如果后续需要将 notices 打包进 release 产物，可以从本文件继续派生

## 待确认项

- `libsndfile` 是否会在未来新增的 CLI 功能路径中成为必须随包分发的 runtime 依赖
- 如果属于分发依赖，应补其许可证标识、上游主页和许可证文本链接
- 如果后续引入新的 CLI crates，需要同步更新本文件与 `docs/legal/cli_third_party_inventory.md`

## 运行时验证备注

- 2026-04-21 的本地运行时隔离验证中，测试包只携带 `FlipBits.exe`、`libstdc++-6.dll`、`libwinpthread-1.dll` 和 `libgcc_s_seh-1.dll`
- 该测试包未携带 `libsndfile-1.dll`
- 在该条件下，`FlipBits version`、`FlipBits licenses`、`FlipBits encode`、`FlipBits decode` 均验证通过
- 因此，本文件当前不把 `libsndfile` 记为已确认的 CLI runtime notices 项
