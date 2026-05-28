# Android 真网络端到端测试

本项目的真网络端到端测试放在 `app/src/androidTest/`，运行在真实 Android 运行环境上，用 Compose UI Test 驱动界面。它用于验证用户从粘贴公开雀魂牌谱链接到生成复盘报告的完整链路。

## 默认策略

- 日常开发默认运行 `./gradlew testDebugUnitTest`。
- 真网络 E2E 不进入默认单元测试门禁，避免外部网络、第三方解析源或牌谱数据变化影响普通开发。
- 真网络 E2E 通过 instrumentation 参数 `runRealNetworkE2E=true` 显式启用；未设置时测试会被 JUnit assumption 跳过。

## 运行命令

连接本机设备或模拟器后运行：

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.runRealNetworkE2E=true
```

使用 Gradle Managed Device 运行：

```bash
./gradlew pixel6Api33DebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.runRealNetworkE2E=true
```

只验证测试 APK 能编译：

```bash
./gradlew assembleDebugAndroidTest
```

## 当前覆盖范围

`RealPaipuImportE2ETest` 覆盖：

1. 启动 `MainActivity`。
2. 在导入输入框填入固定公开雀魂牌谱链接。
3. 点击导入按钮。
4. 等待报告页出现。
5. 断言页面展示“本局结论”和“关键决策点”。

## 维护约定

- 关键 Compose 节点使用 `MahjongCoachTestTags` 中的稳定 tag，避免测试依赖中文文案或布局层级。
- 固定牌谱链接应选择公开、无需登录、可由当前公开解析源解析的牌谱。
- 如果第三方解析源不可用，真网络 E2E 允许失败；不要用它替代本地单元测试和解析器回归测试。
- 涉及 UI 流程改动时，优先更新 `testTag` 和 E2E 断言，而不是让测试通过文本模糊匹配脆弱节点。
