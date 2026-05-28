# 项目 Agent 说明

## 工作规则

1. 除非用户指定，否则不要新建分支；默认在 `main` 分支工作。
2. 代码编写通过测试后，提交到 `main`。
3. 新增文档默认使用中文。
4. 每次修改完代码，都需要验证并调整本文件中的知识图谱，确保模块导航仍然准确。

## 模块导航

- `README.md`：项目入口，说明当前目标、快速运行命令和阶段边界。
- `docs/`：产品与架构文档，包括 Android 架构、复盘报告 schema、跨端契约、牌谱导入方案和探索计划。
- `docs/android-e2e-testing.md`：Android 真网络端到端测试说明，记录 `androidTest`、Managed Device 和显式启用参数。
- `app/`：当前主线 Android 工程，负责雀魂 AI 复盘教练的移动端体验。
- `app/src/main/java/com/example/mahjongcoach/MainActivity.kt`：Compose 应用入口和主要页面展示。
- `app/src/main/java/com/example/mahjongcoach/MahjongCoachTestTags.kt`：Compose 自动化测试使用的稳定节点标识。
- `app/src/main/java/com/example/mahjongcoach/ui/`：UI 状态与 ViewModel，负责把导入、评估结果组织成页面可展示的数据。
- `app/src/main/java/com/example/mahjongcoach/data/`：数据接入层，负责样例读取、牌谱链接解析、公开牌谱下载和外部数据解析。
- `app/src/main/java/com/example/mahjongcoach/domain/`：平台无关领域模型，定义复盘报告、关键决策点、指标和适配器接口。
- `app/src/main/java/com/example/mahjongcoach/evaluator/`：规则型复盘评估逻辑，负责向听、有效牌、危险度、攻守判断和报告生成。
- `app/src/main/assets/`：Android 内置样例牌谱数据。
- `app/src/test/`：Android 本地单元测试，覆盖 data、evaluator、ui 的关键逻辑。
- `app/src/androidTest/`：Android 设备/模拟器测试，当前包含真网络牌谱导入 E2E，用 instrumentation 参数显式启用。
- `strategy_intel/`：Python 策略评估原型，保留雀魂和三国志 11 demo，用于验证报告结构和多游戏扩展方向。
- `samples/`：Python demo 的样例输入。
- `tests/`：Python demo 的最小回归测试。

## 常用验证入口

- Python demo：`python3 -m unittest discover -s tests`
- Android 单元测试：`./gradlew testDebugUnitTest`
- Android 端到端测试编译：`./gradlew assembleDebugAndroidTest`
- Android 真网络 E2E：`./gradlew pixel6Api33DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.runRealNetworkE2E=true`
- Android 构建：`./gradlew assembleDebug`
