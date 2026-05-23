# Mahjong Soul AI Review Coach

一个用于探索 **雀魂 AI 复盘教练** 的 4 周原型项目。

长期母题：构建可迁移的策略局势评估能力，让 AI 能理解复杂策略局势，并把理解转化为玩家成长建议。当前主线是 Android 端雀魂复盘教练，长期方向是 **多游戏教练 AI**。

## 当前主线项目

- **Android 端雀魂 AI 复盘教练**：优先解析雀魂/牌谱屋链接，下载标准化牌谱详情，识别牌谱来源、UUID、视角信息和官方牌谱链接，并用中文样例预览可解释、可训练的复盘报告。

本仓库当前 Python Demo 保留为策略评估原型和报告结构验证层，不代表最终产品形态。Android 工程是第一阶段优先实现形态；规范文档用于支持未来迁移到 Web、桌面、iOS 或服务端。

三国志 11 真人感 AI Mod 原型保留为未来多游戏扩展参考，不再作为第一阶段必须交付 Demo。

## 快速运行

```bash
python3 -m strategy_intel.cli mahjong --input samples/mahjong_round.json
python3 -m strategy_intel.cli san11 --input samples/san11_state.json
python3 -m unittest discover -s tests
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## 项目结构

```text
docs/
  exploration-plan.md          # 雀魂 AI 复盘教练优先探索计划
  product-spec.md              # 雀魂复盘教练产品规格
  review-report-schema.md      # 复盘报告结构规范
  android-architecture.md      # Android 工程分层与边界
  cross-platform-contract.md   # 跨端迁移契约
  candidate-archive.md         # A/B/C 方向归档
samples/
  mahjong_round.json           # 雀魂复盘样例输入
  san11_state.json             # 三国志 11 局势样例输入
app/
  src/main/assets/                # Android 内置中文预览样例
  src/main/java/com/example/mahjongcoach/
    data/                      # 链接识别、样例读取与解析
    domain/                    # 平台无关领域模型
    evaluator/                 # 雀魂复盘评估器
    ui/                        # Compose UI 状态与样例切换
strategy_intel/
  core.py                      # 通用局势评估模型
  mahjong.py                   # 雀魂复盘 Demo
  san11.py                     # 三国志 11 AI 增强 Demo
  cli.py                       # 命令行演示入口
tests/
  test_demos.py                # 最小回归测试
```

## 第一阶段成功标准

4 周结束时必须回答三个问题：

1. 雀魂复盘报告是否真的抓住了值得复盘的点？
2. Android Demo 是否能形成完整、顺手、有教练感的移动端体验？
3. 报告 schema、领域模型和跨端契约是否足够支持未来迁移和多游戏扩展？

## 当前边界

- 不做实时辅助。
- 不做账号登录。
- 不做可能违反平台规则的自动化操作。
- 不提前实现多个游戏，只保留必要的 `GameAdapter` 扩展空间。
