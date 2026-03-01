# apps/audio_android 每次修改后编译流程

## 1. 进入模块目录

```powershell
cd "apps/audio_android"
```

## 2. 每次修改后最小验证（必跑）

Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS/Linux:

```bash
./gradlew :app:assembleDebug
```

## 3. 修改 Gradle/CMake/依赖后（建议跑）

Windows:

```powershell
.\gradlew.bat clean :app:assembleDebug
```

macOS/Linux:

```bash
./gradlew clean :app:assembleDebug
```

## 4. 发版前验证（按需）

Windows:

```powershell
.\gradlew.bat :app:assembleRelease
```

macOS/Linux:

```bash
./gradlew :app:assembleRelease
```

## 5. 编译失败排查

```powershell
.\gradlew.bat :app:assembleDebug --stacktrace
```
