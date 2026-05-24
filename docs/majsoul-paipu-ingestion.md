# 雀魂牌谱获取与解析方案

## Summary

第一阶段建议只支持用户主动提交的公开雀魂牌谱链接，例如：

```text
https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483
```

结论：

- 已知公开牌谱 URL / UUID 时，可以不使用用户账号登录，拿到牌谱细节。
- 不能在不登录的情况下可靠获取某个账号的历史牌谱列表。
- 产品上应避免代管账号、自动登录、批量抓取历史牌谱，降低平台规则和风控风险。
- Android Demo 阶段推荐走服务端或离线中间层解析，不建议在 Android 端直接维护雀魂 websocket/protobuf 细节。

## 牌谱链接结构

普通公开链接的核心结构：

```text
https://game.maj-soul.com/1/?paipu={game_uuid}_a{encoded_account_id}
```

示例：

```text
game_uuid: 260522-2793b6c3-992e-424b-9205-11a121e9ac00
encoded_account_id: 64445483
```

`_a...` 后缀用于指定回放主视角。它不是座位号，而是经过编码的账号 ID。已验证的转换逻辑：

```js
function encodeAccountID(id) {
  return (7 * parseInt(id) + 1117113 ^ 86216345) + 1358437;
}

function decodeAccountID(id) {
  return ((parseInt(id) - 1358437 ^ 86216345) - 1117113) / 7;
}
```

上面的示例中：

```text
decodeAccountID(64445483) = 16329130
```

在牌谱账户列表中找到 `accountId = 16329130`，即可确定主视角玩家和座位。

## 可行方案

### 方案 A：借助第三方公开解析结果

代表站点：

- https://maj.gg/
- https://mjai.ekyu.moe/

`maj.gg` 的牌谱页面会把已解析后的游戏数据嵌入 HTML 的 Fresh state 中。实际验证时，请求：

```text
https://maj.gg/game/{game_uuid}
```

可以拿到包含以下内容的结构化 JSON：

- `accounts`：玩家账号、昵称、座位等。
- `Rounds`：每局数据。
- `tiles0` - `tiles3`：各家配牌。
- `Tile`：按时间顺序排列的摸牌、打牌、副露事件。
- `scores`、`doras`、`paishan`、`finalScores` 等。

优点：

- 实现成本最低。
- 不需要自己维护雀魂协议、protobuf schema、websocket 请求。
- 适合探索期、样例导入、算法原型验证。

缺点：

- 依赖第三方服务稳定性。
- HTML 内嵌状态不是正式 API，字段可能变化。
- 不适合作为长期核心链路。

适合当前阶段：

- Android Demo 前期。
- 本地脚本批量准备样例牌谱。
- 验证复盘报告 schema 和关键决策点识别。

### 方案 B：直接调用雀魂牌谱接口

雀魂客户端回放公开牌谱时，核心调用是：

```text
Lobby.fetchGameRecord
```

核心请求参数通常包括：

- `game_uuid`
- `client_version_string`

返回数据是 protobuf 编码的牌谱详情。解码后可得到完整对局细节，包括：

- 对局元数据。
- 玩家与座位。
- 每局配牌。
- 摸牌、打牌、副露、立直、和了、流局。
- 牌山、宝牌、点数变动等。

优点：

- 更接近原始数据源。
- 长期可控性比爬第三方页面好。
- 能拿到更完整、更标准的牌谱信息。

缺点：

- 需要维护雀魂当前 web 版本、protobuf schema 和请求格式。
- 雀魂协议和资源路径会随版本变化。
- 需要处理网络、风控、接口变化。

适合阶段：

- 确认产品方向后。
- 需要稳定批量解析用户提交的公开链接时。
- 服务端中间层，而不是 Android 客户端直连。

### 方案 C：登录后获取用户牌谱列表

第三方工具通常通过登录后接口获取“用户牌谱列表”，再逐个拉取牌谱详情。

这种方案不适合当前产品边界：

- 需要用户账号登录或凭证。
- 存在账号安全和平台规则风险。
- 容易被误解为代管账号或自动化抓取。
- 对 Android Demo 的核心价值不高。

建议明确不做：

- 不保存用户雀魂账号密码。
- 不做自动登录。
- 不做账号历史牌谱批量同步。
- 不做实时对局辅助。

## 推荐 MVP 链路

第一版建议：

```text
用户粘贴公开牌谱 URL
  -> 提取 game_uuid 和 encoded_account_id
  -> 解码主视角 account_id
  -> 获取解析后的牌谱 JSON
  -> 找到主视角 seat
  -> 遍历 Rounds[].Tile
  -> 输出主视角每巡动作
  -> 生成复盘报告
```

主视角识别：

```text
encoded_account_id -> decodeAccountID -> accountId
accountId -> accounts[].seat
```

每巡出牌提取：

```text
for round in Rounds:
  pending_draw = null
  turn = 0

  for event in round.Tile:
    if event.seat != main_seat:
      continue

    if event.TileType == "Draw":
      turn += 1
      pending_draw = event.tile

    if event.TileType == "Call":
      pending_call = event.tiles

    if event.TileType == "Discard":
      if pending_draw:
        output turn, pending_draw, event.tile, event.moqie
      else if pending_call:
        output call, event.tile
      else:
        output turn, event.tile
```

注意：

- 有些局主视角是庄家，开局第一打可能没有显式 `Draw` 事件。
- `moqie = true` 表示摸切。
- 红 5 通常表示为 `0m`、`0p`、`0s`。
- 字牌通常表示为 `1z` 到 `7z`，对应东、南、西、北、白、发、中。

## 数据格式建议

可以先定义平台无关的中间结构，Android 只消费这个结构：

```json
{
  "game_uuid": "260522-2793b6c3-992e-424b-9205-11a121e9ac00",
  "view_player": {
    "account_id": 16329130,
    "nickname": "南风快乐岛",
    "seat": 2
  },
  "rounds": [
    {
      "round_label": "东1局",
      "actions": [
        {
          "turn": 1,
          "draw": "9s",
          "discard": "9s",
          "is_tsumogiri": true,
          "call": null,
          "riichi": false
        }
      ]
    }
  ]
}
```

后续复盘报告可以引用 `round_label`、`turn`、`draw`、`discard`，再进入 `review-report-schema.md` 中的 `DecisionPoint`。

## 合规和产品边界

建议在产品和代码层都保持这些边界：

- 只解析用户主动提交的公开牌谱链接。
- 不帮助用户实时对局决策。
- 不索要、不保存、不代理用户账号凭证。
- 不批量抓取用户历史牌谱。
- 对外说明本工具用于赛后复盘。
- 服务端解析要限频、缓存、记录失败原因，避免对第三方服务或雀魂接口造成异常流量。

## 当前验证记录

已验证的示例牌谱：

```text
https://game.maj-soul.com/1/?paipu=260522-2793b6c3-992e-424b-9205-11a121e9ac00_a64445483
```

验证结果：

- `game_uuid = 260522-2793b6c3-992e-424b-9205-11a121e9ac00`
- `_a64445483` 解码得到 `accountId = 16329130`
- 牌谱中该账号昵称为 `南风快乐岛`
- 座位为 `2`
- 可通过解析 `Rounds[].Tile` 得到主视角每巡摸牌、出牌、摸切、立直、副露等信息。

## 参考来源

- Majsoul Log Viewer: https://maj.gg/
- Mahjong AI Tools / mjai-reviewer: https://mjai.ekyu.moe/
- mjai-reviewer repository: https://github.com/Equim-chan/mjai-reviewer
- 雀魂 API wiki: https://wikiwiki.jp/majsoul-api/
- 雀魂牌谱链接转换器: https://avenshy.github.io/majsoultools/paipu.html
