package com.chaoscycle.client.screen;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.client.gui.CommandLogWidget;
import com.chaoscycle.network.C2SCommandTreeRequest;
import com.chaoscycle.network.C2SExecuteCommand;
import com.chaoscycle.network.C2SRequestExecutorState;
import com.chaoscycle.network.C2SSetGameMode;
import com.chaoscycle.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 聊天栏式的命令执行器界面。
 *
 * 补全直接使用原版 {@link CommandSuggestions}，包括高亮、候选切换、鼠标选择、
 * 使用提示等全部行为；只在界面打开期间把连接的 suggestion provider 换成一个
 * "hasPermission(<=2) 恒为 true" 的子类，让选择器参数补全（@e[...]）也能工作。
 * 不修改 LocalPlayer 的权限等级；关闭界面时把原 provider 换回。
 *
 * 另外 CommandSuggestions 的 Y 坐标是按原版聊天栏硬编码的
 * （anchorToBottom ? screen.height - 12 : 72），所以布局完成后把 Screen.height
 * 换成假值，让列表落在输入行上方；渲染背景时再临时恢复真实高度。
 */
public class CommandExecutorScreen extends Screen {
    // 418 + 8 边框 = 426，卡在 GUI Scale 2 的 427×240 逻辑分辨率内。
    private static final int PANEL_WIDTH = 418;
    private static final int PANEL_HEIGHT = 228;
    private static final int MODE_BUTTON_WIDTH = 80;
    private static final int MODE_BUTTON_HEIGHT = 20;
    private static final int MODE_BUTTON_GAP = 4;
    private static final int LOG_Y = 62;
    private static final int LOG_HEIGHT = 144;
    private static final int INPUT_Y = 210;
    private static final int INPUT_BOX_Y = 206; // 输入行背景顶边，也是补全列表的下边界
    // 只保留生存 / 创造：旁观模式没有物品栏、右键也无 Item#use，
    // 误切进去后不容易回来，调试道具不做这个入口。
    private static final GameType[] GAME_MODES = {
            GameType.SURVIVAL, GameType.CREATIVE
    };

    /** ClientPacketListener 里唯一一个 ClientSuggestionProvider 类型的字段。 */
    private static final Field SUGGESTIONS_PROVIDER_FIELD = findSuggestionsProviderField();

    private final InteractionHand hand;
    private final Button[] gameModeButtons = new Button[GAME_MODES.length];
    private GameType gameMode = GameType.SURVIVAL;
    private EditBox commandField;
    private CommandSuggestions commandSuggestions;
    private CommandLogWidget logWidget;

    private ClientPacketListener patchedConnection;
    private ClientSuggestionProvider originalSuggestionProvider;

    private String historyBuffer = "";
    private int historyPos;
    private int realHeight;
    private boolean initialized;

    private int left;
    private int top;

    public CommandExecutorScreen(InteractionHand hand) {
        super(Component.translatable("gui.chaoscycle.command_executor.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        // 调整窗口大小会走 resize → rebuildWidgets → init()，同一次屏幕实例被重建。
        // 先把输入内容和输出区内容存下来，重建后再恢复。
        List<FormattedCharSequence> previousLog = this.logWidget == null
                ? null : this.logWidget.snapshotLines();
        int previousLogScroll = this.logWidget == null ? 0 : this.logWidget.getScrolledUp();
        String previousValue = this.commandField == null ? "" : this.commandField.getValue();
        int previousCursor = this.commandField == null ? 0 : this.commandField.getCursorPosition();
        int previousHistoryPos = this.historyPos;
        String previousHistoryBuffer = this.historyBuffer;
        boolean firstInit = !this.initialized;

        // 不要用 this.height：上一次布局后它被改成了补全用的假值。窗口缩放后的真实值从这里取。
        this.realHeight = this.minecraft.getWindow().getGuiScaledHeight();
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.realHeight - PANEL_HEIGHT) / 2;

        int totalWidth = GAME_MODES.length * MODE_BUTTON_WIDTH + (GAME_MODES.length - 1) * MODE_BUTTON_GAP;
        int startX = this.left + (PANEL_WIDTH - totalWidth) / 2;
        for (int i = 0; i < GAME_MODES.length; i++) {
            GameType mode = GAME_MODES[i];
            int x = startX + i * (MODE_BUTTON_WIDTH + MODE_BUTTON_GAP);
            gameModeButtons[i] = this.addRenderableWidget(Button.builder(modeLabel(mode), button -> {
                this.gameMode = mode;
                refreshGameModeButtons();
                ModNetwork.sendToServer(new C2SSetGameMode(this.hand.ordinal(), mode.getId()));
            }).bounds(x, this.top + 34, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT).build());
        }

        // 无边框 EditBox：只作为文本/光标模型和 CommandSuggestions 的锚点。
        this.commandField = new EditBox(this.font,
                this.left + 26, this.top + INPUT_Y, 376, 12,
                Component.translatable("gui.chaoscycle.command.hint"));
        this.commandField.setBordered(false);
        this.commandField.setMaxLength(256);
        this.commandField.setHint(Component.translatable("gui.chaoscycle.command.hint"));
        this.commandField.setFocused(true);
        this.commandField.setResponder(value -> {
            if (this.commandSuggestions != null) {
                this.commandSuggestions.setAllowSuggestions(!value.isEmpty());
                this.commandSuggestions.updateCommandInfo();
            }
        });

        this.logWidget = this.addRenderableWidget(new CommandLogWidget(
                this.left + 16, this.top + LOG_Y, 386, LOG_HEIGHT, Component.empty()));
        if (previousLog != null && !previousLog.isEmpty()) {
            this.logWidget.restoreSnapshot(previousLog, previousLogScroll);
        } else {
            this.logWidget.append(Component.translatable("gui.chaoscycle.command_executor.hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // CommandSuggestions 的 Tab 补全要求 screen.getFocused() == input。
        this.setFocused(this.commandField);

        // 界面打开期间把 suggestion provider 换成固定 2 级权限的版本。
        // init() 可能因窗口缩放被再次调用：只在第一次替换时保存原 provider，
        // 否则会把已经替换过的实例当成"原实例"存下来，关闭时无法恢复。
        Minecraft minecraft = Minecraft.getInstance();
        if (this.patchedConnection == null
                && minecraft.player != null && minecraft.player.connection != null) {
            this.patchedConnection = minecraft.player.connection;
            this.originalSuggestionProvider = this.patchedConnection.getSuggestionsProvider();
            setSuggestionProvider(this.patchedConnection,
                    new FixedPermissionSuggestionProvider(this.patchedConnection, minecraft, 2));
        }

        // 让原版补全列表的底边正好落在输入行上方：
        // showSuggestions 使用的 Y = screen.height - 12，SuggestionsList 再上移 3 + 行高。
        this.height = this.top + INPUT_BOX_Y + 15;

        // commandsOnly=false：和原版聊天栏一样，只有以 / 开头时才按指令补全。
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.commandField,
                this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();

        // 恢复缩放前正在输入的内容和光标位置。
        if (!previousValue.isEmpty()) {
            this.commandField.setValue(previousValue);
            this.commandField.setCursorPosition(Math.min(previousCursor, previousValue.length()));
            this.commandSuggestions.updateCommandInfo();
        }

        if (firstInit) {
            this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
            this.historyBuffer = "";
        } else {
            this.historyPos = previousHistoryPos;
            this.historyBuffer = previousHistoryBuffer;
        }
        this.initialized = true;

        refreshGameModeButtons();
        ModNetwork.sendToServer(new C2SRequestExecutorState(this.hand.ordinal()));
        // 补全使用的命令树也要切到 2 级权限，关闭界面时再恢复玩家自己的命令树。
        ModNetwork.sendToServer(new C2SCommandTreeRequest(this.hand.ordinal(), true));
    }

    @Override
    public void removed() {
        if (this.patchedConnection != null && this.originalSuggestionProvider != null
                && this.minecraft.player != null
                && this.minecraft.player.connection == this.patchedConnection) {
            setSuggestionProvider(this.patchedConnection, this.originalSuggestionProvider);
        }
        this.patchedConnection = null;
        this.originalSuggestionProvider = null;
        ModNetwork.sendToServer(new C2SCommandTreeRequest(this.hand.ordinal(), false));
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // 布局完成后 height 被改成了假值；背景仍要按真实窗口高度绘制。
        int fakeHeight = this.height;
        this.height = this.realHeight > 0 ? this.realHeight : this.minecraft.getWindow().getGuiScaledHeight();
        super.renderBackground(graphics);
        this.height = fakeHeight;
    }

    private void refreshGameModeButtons() {
        for (int i = 0; i < GAME_MODES.length && i < gameModeButtons.length; i++) {
            Button button = gameModeButtons[i];
            if (button == null) {
                continue;
            }
            boolean selected = GAME_MODES[i] == this.gameMode;
            button.setMessage(Component.literal(selected ? "▶ " : "").append(modeLabel(GAME_MODES[i])));
        }
    }

    private static Component modeLabel(GameType mode) {
        return Component.translatable("gui.chaoscycle.gamemode." + mode.getName());
    }

    public void applyGameMode(int gameModeId) {
        GameType mode = GameType.byId(gameModeId);
        if (mode != null) {
            this.gameMode = mode;
            refreshGameModeButtons();
        }
    }

    // ------------------------------------------------------------
    // 原版聊天输入行为
    // ------------------------------------------------------------

    /** 上下键调用最近消息，语义与 ChatScreen#moveInHistory 一致。 */
    public void moveInHistory(int direction) {
        List<String> recent = this.minecraft.gui.getChat().getRecentChat();
        int size = recent.size();
        int target = Mth.clamp(this.historyPos + direction, 0, size);
        if (target == this.historyPos) {
            return;
        }
        if (target == size) {
            this.historyPos = size;
            this.commandField.setValue(this.historyBuffer);
            return;
        }
        if (this.historyPos == size) {
            this.historyBuffer = this.commandField.getValue();
        }
        this.commandField.setValue(recent.get(target));
        this.commandSuggestions.setAllowSuggestions(false);
        this.historyPos = target;
    }

    private void execute() {
        String input = this.commandField.getValue().strip();
        if (input.isEmpty()) {
            if (this.logWidget != null) {
                this.logWidget.append(Component.translatable("message.chaoscycle.command.empty")
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }
        boolean isCommand = input.startsWith("/");
        if (isCommand && this.logWidget != null) {
            // 指令只回显命令行；执行结果由服务端 S2CCommandOutput 追加。
            this.logWidget.append(Component.literal("> " + input).withStyle(ChatFormatting.GRAY));
        }
        this.minecraft.gui.getChat().addRecentChat(input);
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        this.historyBuffer = "";
        // 与原版聊天一致：执行后清空输入。
        this.commandField.setValue("");
        if (isCommand) {
            ModNetwork.sendToServer(new C2SExecuteCommand(this.hand.ordinal(), input.substring(1)));
        } else if (this.minecraft.player != null) {
            // 普通文本走原版聊天；发送后服务端会广播回来，由 ClientChatReceivedEvent 追加到输出区。
            this.minecraft.player.connection.sendChat(input);
        }
    }

    /** 非系统聊天（玩家发言、/say 等）实时追加到输出区，使这里像一个聊天终端。 */
    public void appendChat(Component message) {
        if (this.logWidget != null && message != null) {
            this.logWidget.append(message);
        }
    }

    /** 服务端把 sendSuccess / sendFailure 的文本抄送回来，按聊天栏方式追加到输出区。 */
    public void appendCommandOutput(List<Component> messages) {
        if (this.logWidget == null) {
            return;
        }
        for (Component message : messages) {
            this.logWidget.append(message);
        }
    }

    public void setStatus(Component status) {
        if (this.logWidget != null && status != null) {
            this.logWidget.append(status);
        }
    }

    // ------------------------------------------------------------
    // 渲染与输入
    // ------------------------------------------------------------

    @Override
    public void tick() {
        if (this.commandField != null) {
            this.commandField.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(this.left - 4, this.top - 4,
                this.left + PANEL_WIDTH + 4, this.top + PANEL_HEIGHT + 4, 0xCC101010);
        graphics.fill(this.left - 4, this.top - 4,
                this.left + PANEL_WIDTH + 4, this.top - 3, 0xFF5A5A5A);
        graphics.fill(this.left - 4, this.top + PANEL_HEIGHT + 3,
                this.left + PANEL_WIDTH + 4, this.top + PANEL_HEIGHT + 4, 0xFF5A5A5A);

        graphics.drawString(this.font, this.title, this.left + 16, this.top + 12, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.chaoscycle.mode.label"),
                this.left + 16, this.top + 24, 0xAAAAAA, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        // 聊天栏式输入行：半透明底 + 顶部分隔线 + 提示符。
        graphics.fill(this.left + 12, this.top + INPUT_BOX_Y,
                this.left + PANEL_WIDTH - 12, this.top + INPUT_BOX_Y + 18, 0x80000000);
        graphics.fill(this.left + 12, this.top + INPUT_BOX_Y,
                this.left + PANEL_WIDTH - 12, this.top + INPUT_BOX_Y + 1, 0xFF3A3A3A);
        graphics.drawString(this.font, ">", this.left + 16, this.top + INPUT_Y - 1, 0xFFAAAAAA, false);
        this.commandField.render(graphics, mouseX, mouseY, partialTick);

        // 原版补全/用法提示最后画，盖在输出区与输入行之上。
        this.commandSuggestions.render(graphics, mouseX, mouseY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        this.setFocused(this.commandField);
        this.commandField.setFocused(true);
        return this.commandField.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // CommandSuggestions 的 Tab 判定依赖 screen.getFocused() == input。
        this.setFocused(this.commandField);
        this.commandField.setFocused(true);

        if (this.commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveInHistory(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveInHistory(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            execute();
            return true;
        }
        if (this.commandField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.commandSuggestions.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseX >= this.left + 12 && mouseX < this.left + PANEL_WIDTH - 12
                && mouseY >= this.top + INPUT_BOX_Y && mouseY < this.top + INPUT_BOX_Y + 18) {
            this.setFocused(this.commandField);
            this.commandField.setFocused(true);
            this.commandField.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.commandSuggestions.mouseScrolled(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    // ------------------------------------------------------------
    // 固定权限的 suggestion provider
    // ------------------------------------------------------------

    /**
     * 只覆盖权限判断：2 级及以下一律返回 true，其余交给原 provider。
     * 用于通过 EntityArgument.listSuggestions → ForgeHooks.canUseEntitySelectors 的检查。
     */
    private static final class FixedPermissionSuggestionProvider extends ClientSuggestionProvider {
        private final int fixedLevel;

        private FixedPermissionSuggestionProvider(ClientPacketListener connection, Minecraft minecraft,
                                                  int fixedLevel) {
            super(connection, minecraft);
            this.fixedLevel = fixedLevel;
        }

        @Override
        public boolean hasPermission(int level) {
            return level <= this.fixedLevel || super.hasPermission(level);
        }
    }

    private static Field findSuggestionsProviderField() {
        for (Field field : ClientPacketListener.class.getDeclaredFields()) {
            if (ClientSuggestionProvider.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return field;
                } catch (RuntimeException exception) {
                    return null;
                }
            }
        }
        return null;
    }

    private static void setSuggestionProvider(ClientPacketListener connection, ClientSuggestionProvider provider) {
        if (SUGGESTIONS_PROVIDER_FIELD == null) {
            ChaosCycle.LOGGER.warn("ClientPacketListener suggestion provider field not found");
            return;
        }
        try {
            SUGGESTIONS_PROVIDER_FIELD.set(connection, provider);
        } catch (ReflectiveOperationException exception) {
            ChaosCycle.LOGGER.warn("Failed to swap client suggestion provider", exception);
        }
    }
}
