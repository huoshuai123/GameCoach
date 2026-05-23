# 复盘报告结构规范

## 目标

复盘报告 schema 是 Android、Python 原型和未来其他端之间的共同契约。它描述报告应该表达什么，而不是某个平台应该如何展示。

## 顶层结构

`EvaluationReport` 包含：

- `situation`：本局或局部局势的基本信息。
- `summary`：面向玩家的简短总结。
- `metrics`：局势指标列表。
- `decision_points`：关键决策点列表。
- `training_focus`：下一阶段训练重点。

## Situation

字段：

- `game`：游戏名称。雀魂使用 `Mahjong Soul`。
- `title`：对局或报告标题。
- `player`：复盘视角玩家。
- `context`：局、场风、本场、供托、点数等上下文。第一阶段可按样例数据逐步补齐。

要求：

- 不包含 Android UI 状态。
- 不包含平台专属文件路径。
- 可被序列化为 JSON。

## Metric

字段：

- `name`：指标名称，例如打牌效率、危险度、攻守判断。
- `value`：数值化评分，建议使用 0-1 区间。
- `explanation`：为什么给出该评分。

要求：

- 指标数量保持克制，优先服务报告理解。
- 指标必须能和至少一个关键决策点建立解释关系。

## DecisionPoint

字段：

- `turn`：巡目或局面序号。
- `choice`：玩家实际选择。
- `recommendation`：推荐选择或推荐思路。
- `problem_type`：问题类型，例如效率、危险度、攻守判断。
- `reason`：推荐原因。
- `training_tip`：下一次可执行的训练建议。
- `priority`：优先级，建议使用 high、medium、low。

要求：

- 每局报告默认输出 3-5 个关键决策点。
- 每个点必须解释“为什么这个点值得复盘”。
- `training_tip` 必须具体，避免只写“注意防守”这类泛化建议。

## Training Focus

字段：

- `theme`：本局最重要的训练主题。
- `next_action`：下一局可执行动作。
- `evidence`：来自哪些指标或决策点。

要求：

- 优先输出 1-2 条训练重点。
- 训练重点必须来自报告内容，不能凭空生成。

## 兼容性规则

- 新增字段必须向后兼容，旧客户端可以忽略未知字段。
- 字段含义一旦进入 Android Demo，不随意重命名。
- 游戏特有字段放入 `context` 或游戏 adapter 输出中，避免污染通用报告结构。
- 展示层可以调整排序和布局，但不得改变 schema 的语义。
