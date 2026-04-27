## 编译失败修复非法转义
python tools/scripts/android/translate/run.py fix-resource-escapes --quiet

会自动把这类非法 Android string 修正成合法形式
例如：
d'accento -> d\'accento
Необов'язково -> Необов\'язково
