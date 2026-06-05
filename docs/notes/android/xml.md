## 编译失败修复非法转义
python tools/run.py android-translate fix-resource-escapes --quiet

项目级说明见：
`docs/design/android/translation/android-translate-command-guide.md`

会自动把这类非法 Android string 修正成合法形式
例如：
d'accento -> "d'accento"
d\'accento -> "d'accento"
Необов'язково -> "Необов'язково"
