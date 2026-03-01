# Binary_Audio_Generator
用于把文字转化为二进制音频（FSK）。

## 依赖安装
```
pip install -r python/requirements.txt
```

## 使用示例
编码：
```
python python/main.py encode --text "Hello" --out data/output_audio/hello.wav
```

从文本文件编码：
```
python python/main.py encode --text-file data/input.txt --out data/output_audio/hello.wav
```

解码：
```
python python/main.py decode --in data/output_audio/output.wav
```

解码并写入文本文件：
```
python python/main.py decode --in data/output_audio/output.wav --out-text data/output_text/output.txt
```

## C++ 版本（libsndfile + KissFFT）
内核版本：`0.1.0`（详见 [docs/core.md](./docs/core.md)）
表现层版本：`0.1.0`（详见 [docs/presentation.md](./docs/presentation.md)）

### 依赖（MSYS2 UCRT64）
```
pacman -S --needed \
  mingw-w64-ucrt-x86_64-gcc \
  mingw-w64-ucrt-x86_64-cmake \
  mingw-w64-ucrt-x86_64-ninja \
  mingw-w64-ucrt-x86_64-pkgconf \
  mingw-w64-ucrt-x86_64-libsndfile \
  mingw-w64-ucrt-x86_64-kissfft
```

### 构建
```
cmake -S . -B build -G Ninja
cmake --build build
```

### 使用示例
编码：
```
build/binary_audio_cpp.exe encode --text "Hello" --out data/output_audio/hello.wav
```

从文本文件编码：
```
build/binary_audio_cpp.exe encode --text-file data/input.txt --out data/output_audio/hello.wav
```

解码：
```
build/binary_audio_cpp.exe decode --in data/output_audio/output.wav
```

解码并写入文本文件：
```
build/binary_audio_cpp.exe decode --in data/output_audio/output.wav --out-text data/output_text/output.txt
```

查看 CLI 相关第三方许可证：
```
build/binary_audio_cpp.exe licenses
```

查看 CLI 与内核版本：
```
build/binary_audio_cpp.exe version
```


## 致谢
本项目使用了以下第三方库，感谢其贡献：
- libsndfile
- KissFFT
- NumPy
- SciPy

## Licenses
- 本项目：MIT（见 [LICENSE](./LICENSE)）
- libsndfile：LGPL-2.1-or-later
- KissFFT：BSD-3-Clause
- NumPy：BSD-3-Clause
- SciPy：BSD-3-Clause

说明：第三方库许可证请以上游项目仓库中的 LICENSE 文件为准。
