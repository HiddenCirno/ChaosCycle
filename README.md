# Chaos Cycle / 混沌时序

**Minecraft 1.20.1 / Forge 47.x** 的调试工具模组。

目前包含两个**无法合成**的调试物品：

| 物品 | 注册名 | 用途 |
|---|---|---|
| 便携式命令执行器 | `chaoscycle:command_executor` | 切换游戏模式；以固定 2 级权限执行指令 |
| 便携式超空间定位器 | `chaoscycle:hyperspace_locator` | 存储坐标列表，编辑条目并跨维度传送 |

## 获取

```mcfunction
/give @s chaoscycle:command_executor
/give @s chaoscycle:hyperspace_locator
```

也可以在**创造模式 → 管理员用品（OP Blocks）**标签页里找到。

## 便携式命令执行器

右键打开；或手持时按 **V**（默认快捷键，可在按键设置里改）。

- 两个按钮对应：**生存 / 创造**。
  点击后由服务端调用 `ServerPlayer#setGameMode` 切换（它会下发 `ClientboundGameEventPacket.CHANGE_GAME_MODE`，
  客户端才能同步模式；只调 `changeGameModeForPlayer` 会导致服务端切了、客户端不知道）。
- 不提供冒险 / 旁观入口：旁观没有物品栏且右键失效，切进去后不容易回来。
- 界面底部是一行**聊天栏式输入**：直接打字即可，没有独立输入框。指令不需要前导 `/`，长度上限 256。
- 输入行为直接复用原版聊天框：
  - Tab 补全 / Shift+Tab 反向切换、↑/↓ 在候选间切换、鼠标悬停高亮与点击选中；
  - 补全后列表保持展开，可以继续修改命令，直到按 Enter 确认；
  - Enter 执行后自动清空输入；
  - ↑/↓ 在补全列表未展开时调用原版最近消息历史（`ChatComponent#getRecentChat`）。
- 补全列表紧贴输入行上方。原版 `CommandSuggestions` 的 Y 坐标是给聊天栏写死的，
  这里在布局完成后替换 `Screen.height` 让硬编码坐标落到正确位置，渲染背景时再恢复真实高度。
- 补全使用**固定的 2 级权限命令树**，与执行权限一致：打开界面时服务端按
  `createCommandSourceStack().withPermission(2)` 过滤并下发命令树，权限 0 的玩家也能补全
  `/give`、`/tp`；关闭界面时恢复玩家原本的命令树。
- **指令固定以 2 级权限执行**：权限 0 的玩家也能执行 `/gamemode`、`/give`、`/tp` 等 2 级指令；
  但最高只有 2 级，无法通过本道具执行 `/op`、`/stop` 等 3/4 级命令。
- GUI 下方有**聊天栏式输出区**：执行时先回显 `> /命令`，随后实时显示指令的全部反馈
  （`sendSuccess` / `sendFailure` 文本，失败为红色）；支持自动换行、贴底滚动、滚轮上翻。
- 反馈同时照常转发到玩家聊天栏。

实现上使用：

```java
player.createCommandSourceStack().withPermission(2)
```

## 便携式超空间定位器

右键打开；或手持时按 **V**。

- **存储位置是玩家自身**（Forge `player.getPersistentData()`），不写物品 NBT：
  换一把定位器、道具丢失/重生都不会丢坐标；同一玩家的所有定位器共享同一份列表。
- **坐标列表**：显示所有已保存条目，可滚动、点击选中。有名字时显示 `#n 名字 · 维度`，
  未命名时按原格式显示 `#n 维度 @ x, y, z`。
- **保存当前坐标**：把玩家当前维度 + 坐标 + 朝向加入列表（最多 32 条），自动选中新条目；
  名称输入框有内容就一并保存。
- 选中条目后：
  - 名称输入框可重命名（留空即恢复默认显示格式）；
  - X/Y/Z 输入框修改坐标；
  - **保存修改** 写回该条目（维度保持不变）；
  - **删除** 移除该条目；
  - **传送** 传送到该条目，**不需要任何权限**。
- **取当前坐标**：把玩家当前位置填入输入框，方便修改条目。
- 坐标校验在**保存、修改、传送**三处都会由服务端执行：
  - 条目保存的维度必须仍存在（`server.getLevel(...)` 非空）；
  - 坐标必须是有限值；
  - Y 必须在目标维度高度范围内；
  - 必须位于世界边界内。
- 传送使用 `ServerPlayer#teleportTo(..., Set.of(), yaw, pitch)`，保留同一个玩家对象并触发维度事件。
  跨维度传送后会补回飞行状态并重新下发能力包，避免飞到半空突然坠落。

玩家持久化数据（`ForgeData` 里）：

```nbt
{
  ForgeData: {
    ChaosCycleWaypoints: [
      {
        Dimension: "minecraft:overworld",
        X: 0.0d, Y: 64.0d, Z: 0.0d,
        Yaw: 0.0f, Pitch: 0.0f
      }
    ]
  }
}
```

死亡/末地返回会新建 `ServerPlayer`，Forge 只自动搬运 `PlayerPersisted` 子标签，
所以 `PlayerEvent.Clone` 里显式复制了 `ChaosCycleWaypoints`。

## 构建

```bash
./gradlew build
```

产物：`build/libs/chaoscycle-forge-1.20.1-0.1.0+mc1.20.1.jar`

开发端：

```bash
./gradlew runClient
```

## 说明

- 纹理是 16×16 占位图，后续可替换。
- 旁观者模式下原版右键不会触发 `Item#use`，请用 **V** 键打开 GUI。
- 命令执行器会把指令源提升到 2 级，这是刻意设计；不要给不应拥有该能力的玩家发放此道具。
- 模组采用零 Mixin；两个机制都通过公开 API / 事件 / 数据包实现。
