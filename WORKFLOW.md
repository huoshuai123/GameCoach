---
tracker:
  kind: linear
  # TODO: Start Symphony with both variables set:
  #   export LINEAR_API_KEY="your-linear-personal-api-key"
  #   export LINEAR_PROJECT_SLUG="your-linear-project-slug"
  #
  # Linear project slug example:
  #   https://linear.app/acme/project/gamecoach-123abc
  #   LINEAR_PROJECT_SLUG=gamecoach-123abc
  project_slug: $LINEAR_PROJECT_SLUG
  api_key: $LINEAR_API_KEY
  active_states:
    - Todo
    - In Progress
    - Rework
  terminal_states:
    - Done
    - Closed
    - Cancelled
    - Canceled
    - Duplicate
polling:
  interval_ms: 30000
workspace:
  root: /Users/bytedance/Documents/Codex/symphony-workspaces/mahjongcoach
hooks:
  after_create: |
    git clone git@huoshuai123:huoshuai123/GameCoach.git .
    git switch main
    git pull --ff-only origin main
agent:
  max_concurrent_agents: 1
  max_turns: 12
codex:
  command: /Applications/Codex.app/Contents/Resources/codex --config shell_environment_policy.inherit=all app-server
  thread_sandbox: workspace-write
---

你正在处理一个 Android/Kotlin/Jetpack Compose 项目的 Linear 任务。

项目路径：/Users/bytedance/Documents/Codex/2026-05-23/ai-1-android-2-3-4
远端仓库：git@huoshuai123:huoshuai123/GameCoach.git

当前任务：
{{ issue.identifier }} - {{ issue.title }}

任务状态：{{ issue.state }}
任务链接：{{ issue.url }}

任务描述：
{% if issue.description %}
{{ issue.description }}
{% else %}
无任务描述。先根据标题做最小必要分析；如果缺少关键需求或凭据，明确记录阻塞原因。
{% endif %}

工作规则：

1. 默认在 `main` 分支工作，除非任务明确要求新建分支。
2. 新增文档默认使用中文。
3. 保持改动聚焦当前 Linear 任务，不做无关重构。
4. 代码风格跟随当前项目：Kotlin、Gradle Kotlin DSL、Jetpack Compose、JUnit4。
5. 修改代码后至少运行 `./gradlew test`。
6. 涉及构建配置、依赖或 UI 的任务，额外运行 `./gradlew assembleDebug`。
7. 测试通过后提交到 `main` 并推送到远端。
8. 如果遇到凭据、网络、Linear/GitHub 权限或外部服务阻塞，停止扩大改动，清楚记录：
   - 缺少什么；
   - 为什么阻塞；
   - 需要人工执行的具体动作。

推荐执行流程：

1. 读取 `AGENTS.md`、`README.md`、相关源码和测试，确认当前项目约束。
2. 先复现或确认任务涉及的现状，再修改代码。
3. 对高风险逻辑优先补测试，再实现。
4. 运行必要验证命令，并在最终回复里列出验证结果。
5. 提交信息使用简洁中文或英文，能说明本次任务目的即可。

最终回复必须包含：

- 完成的改动；
- 运行过的验证命令及结果；
- 提交 hash；
- 如有阻塞，列出阻塞原因和需要人工处理的动作。
