package com.chaoscycle.client.screen;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.client.gui.CommandExecutorGui;
import com.chaoscycle.client.gui.CommandTerminalWidget;
import com.chaoscycle.client.gui.ExtraButton;
import com.chaoscycle.client.gui.ModeButton;
import com.chaoscycle.network.C2SCommandTreeRequest;
import com.chaoscycle.network.C2SExecuteCommand;
import com.chaoscycle.network.ModNetwork;
import com.chaoscycle.util.ItemSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 命令执行器界面。
 *
 * 主体 247x167；输入区内区 210x150（14 行日志 + 1 行输入）；
 * 右侧 4 个 22x22 模式按钮；主体上方 4 个 20x20 页面按钮。
 *
 * 输入行使用原版 CommandSuggestions；打开界面期间把连接的 suggestion provider
 * 换成固定 2 级权限的版本，关闭时恢复。CommandSuggestions 的 Y 坐标按原版
 * 聊天栏的 screen.height 计算，因此给它一个专用锚点 Screen，而不修改真实界面高度。
 */
public class CommandExecutorScreen extends Screen {
    private static final GameType[] GAME_MODES = {
            GameType.CREATIVE, GameType.SURVIVAL, GameType.ADVENTURE, GameType.SPECTATOR
    };
    private static final ItemStack[] MODE_ICONS = {
            new ItemStack(Items.GRASS_BLOCK),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.PAPER),
            new ItemStack(Items.ENDER_EYE)
    };

    /** ClientPacketListener 里唯一一个 ClientSuggestionProvider 类型的字段。 */
    private static final Field SUGGESTIONS_PROVIDER_FIELD = findSuggestionsProviderField();

    private final ItemSource source;
    private final ModeButton[] modeButtons = new ModeButton[GAME_MODES.length];
    private final ExtraButton[] pageButtons = new ExtraButton[4];

    private CommandTerminalWidget logWidget;
    private EditBox commandField;
    private CommandSuggestions commandSuggestions;

    private ClientPacketListener patchedConnection;
    private ClientSuggestionProvider originalSuggestionProvider;

    private String historyBuffer = "";
    private int historyPos;
    private boolean initialized;

    private int left;
    private int top;

    public CommandExecutorScreen(ItemSource source) {
        super(Component.translatable("gui.chaoscycle.command_executor.title"));
        this.source = source;
    }

    @Override
    protected void init() {
        // 调整窗口大小会重建界面；把输入/日志/历史带过去。
        List<FormattedCharSequence> previousLog = this.logWidget == null
                ? null : this.logWidget.snapshotLines();
        int previousLogScroll = this.logWidget == null ? 0 : this.logWidget.getScrolledUp();
        String previousValue = this.commandField == null ? "" : this.commandField.getValue();
        int previousCursor = this.commandField == null ? 0 : this.commandField.getCursorPosition();
        boolean previousFocused = this.commandField != null && this.commandField.isFocused();
        int previousHistoryPos = this.historyPos;
        String previousHistoryBuffer = this.historyBuffer;
        boolean firstInit = !this.initialized;

        super.init();

        this.left = (this.width - CommandExecutorGui.BODY_WIDTH) / 2;
        this.top = (this.height - CommandExecutorGui.BODY_HEIGHT) / 2;

        this.logWidget = this.addRenderableWidget(new CommandTerminalWidget(
                left + CommandExecutorGui.INPUT_INNER_X,
                top + CommandExecutorGui.INPUT_INNER_Y,
                CommandExecutorGui.INPUT_INNER_WIDTH,
                CommandExecutorGui.LOG_LINES * CommandExecutorGui.LINE_HEIGHT,
                Component.empty()));
        if (previousLog != null && !previousLog.isEmpty()) {
            this.logWidget.restoreSnapshot(previousLog, previousLogScroll);
        } else {
            this.logWidget.append(Component.translatable("gui.chaoscycle.command_executor.hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // 输入行：输入区最后一行，无边框、无分隔线。
        this.commandField = this.addRenderableWidget(new EditBox(this.font,
                left + CommandExecutorGui.INPUT_TEXT_X,
                top + CommandExecutorGui.INPUT_LINE_Y,
                CommandExecutorGui.INPUT_TEXT_WIDTH, 10,
                Component.empty()));
        this.commandField.setBordered(false);
        this.commandField.setMaxLength(256);
        this.commandField.setHint(Component.translatable("gui.chaoscycle.command.hint"));
        this.commandField.setTextColor(0xFFF0F0F0);
        // 不自动聚焦：避免切到其它窗口后键盘输入继续落进命令框。
        this.commandField.setResponder(value -> {
            if (this.commandSuggestions != null) {
                this.commandSuggestions.setAllowSuggestions(
                        !value.isEmpty() && this.commandField.isFocused());
                this.commandSuggestions.updateCommandInfo();
            }
        });

        for (int i = 0; i < GAME_MODES.length; i++) {
            GameType mode = GAME_MODES[i];
            this.modeButtons[i] = this.addRenderableWidget(new ModeButton(
                    left + CommandExecutorGui.MODE_X,
                    top + CommandExecutorGui.MODE_Y[i],
                    mode, MODE_ICONS[i].copy(),
                    selected -> ModNetwork.sendToServer(new C2SExecuteCommand(
                            this.source, "gamemode " + selected.getName()))));
        }

        // 页面按钮在贴图里位于主体下方；按设计镜像到主体上方 2px。
        this.pageButtons[0] = this.addRenderableWidget(new ExtraButton(
                left + CommandExecutorGui.PAGE_X,
                top + CommandExecutorGui.PAGE_Y, true,
                Blocks.END_GATEWAY.defaultBlockState(),
                Component.translatable("gui.chaoscycle.page.command_executor"), () -> {
        }));
        this.pageButtons[1] = this.addRenderableWidget(new ExtraButton(
                left + CommandExecutorGui.PAGE_X + CommandExecutorGui.PAGE_STEP,
                top + CommandExecutorGui.PAGE_Y, false,
                new ItemStack(Items.COMMAND_BLOCK),
                Component.translatable("gui.chaoscycle.page.pulse"), () -> {
        }));
        this.pageButtons[2] = this.addRenderableWidget(new ExtraButton(
                left + CommandExecutorGui.PAGE_X + CommandExecutorGui.PAGE_STEP * 2,
                top + CommandExecutorGui.PAGE_Y, false,
                new ItemStack(Items.REPEATING_COMMAND_BLOCK),
                Component.translatable("gui.chaoscycle.page.loop"), () -> {
        }));
        this.pageButtons[3] = this.addRenderableWidget(new ExtraButton(
                left + CommandExecutorGui.PAGE_X + CommandExecutorGui.PAGE_STEP * 3,
                top + CommandExecutorGui.PAGE_Y, false,
                new ItemStack(Items.CHAIN_COMMAND_BLOCK),
                Component.translatable("gui.chaoscycle.page.chain"), () -> {
        }));

        // 调整窗口大小会重建控件；只有原来就处于聚焦状态时才恢复焦点。
        if (previousFocused) {
            focusCommandField();
        }

        // 界面打开期间把 suggestion provider 换成固定 2 级权限的版本。
        // init() 可能因窗口缩放再次调用：只在第一次替换时保存原 provider。
        Minecraft minecraft = Minecraft.getInstance();
        if (this.patchedConnection == null
                && minecraft.player != null && minecraft.player.connection != null) {
            this.patchedConnection = minecraft.player.connection;
            this.originalSuggestionProvider = this.patchedConnection.getSuggestionsProvider();
            setSuggestionProvider(this.patchedConnection,
                    new FixedPermissionSuggestionProvider(this.patchedConnection, minecraft, 2));
        }

        // 原版补全列表底边 = screen.height - 12。不能改 this.height：EMI 等
        // 外部模组会直接用 screen.height 计算侧栏/排除区域，改了会让整个覆盖层错位。
        Screen suggestionAnchor = new SuggestionAnchorScreen(
                this.width, top + CommandExecutorGui.INPUT_LINE_Y + 15);

        this.commandSuggestions = new CommandSuggestions(this.minecraft, suggestionAnchor,
                this.commandField, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();

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

        ModNetwork.sendToServer(new C2SCommandTreeRequest(this.source, true));
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
        ModNetwork.sendToServer(new C2SCommandTreeRequest(this.source, false));
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        graphics.blit(CommandExecutorGui.TEXTURE, this.left, this.top, 0, 0,
                CommandExecutorGui.BODY_WIDTH, CommandExecutorGui.BODY_HEIGHT,
                CommandExecutorGui.TEXTURE_WIDTH, CommandExecutorGui.TEXTURE_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 窗口失焦时立刻释放输入焦点；否则在其它窗口打字时，残留的 EditBox
        // 焦点仍可能接收输入（尤其是 IME / 输入法重定向）。
        syncWindowFocus();
        // Screen#render 只遍历 renderables，不会自动调用 renderBackground；
        // 普通 Screen 必须自己先画背景，否则只剩按钮可见。
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // 输入行提示符；输入区内区是纯黑，不画分隔线。
        graphics.drawString(this.font, ">", this.left + CommandExecutorGui.INPUT_INNER_X + 1,
                this.top + CommandExecutorGui.INPUT_LINE_Y + 1, 0xFFAAAAAA, false);
        renderButtonTooltips(graphics, mouseX, mouseY);

        if (this.commandSuggestions != null) {
            this.commandSuggestions.render(graphics, mouseX, mouseY);
        }
    }

    private void renderButtonTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ModeButton button : this.modeButtons) {
            if (button != null && button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(this.font, button.tooltip(), mouseX, mouseY);
                return;
            }
        }
        for (ExtraButton button : this.pageButtons) {
            if (button != null && button.isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(this.font, button.tooltip(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (isTerminal(mouseX, mouseY)) {
            focusCommandField();
            return true;
        }
        return handled;
    }

    private boolean isTerminal(double mouseX, double mouseY) {
        return mouseX >= this.left + CommandExecutorGui.INPUT_OUTER_X
                && mouseX < this.left + CommandExecutorGui.INPUT_OUTER_X
                + CommandExecutorGui.INPUT_OUTER_WIDTH
                && mouseY >= this.top + CommandExecutorGui.INPUT_OUTER_Y
                && mouseY < this.top + CommandExecutorGui.INPUT_OUTER_Y
                + CommandExecutorGui.INPUT_OUTER_HEIGHT;
    }

    /** 窗口失焦时丢弃 EditBox 焦点，避免输入法/键盘事件继续写进命令框。 */
    private void syncWindowFocus() {
        if (this.commandField != null && !this.minecraft.isWindowActive()
                && this.commandField.isFocused()) {
            clearCommandFocus();
        }
    }

    private void clearCommandFocus() {
        if (this.commandField != null) {
            this.commandField.setFocused(false);
        }
        this.setFocused(null);
        if (this.commandSuggestions != null) {
            this.commandSuggestions.setAllowSuggestions(false);
            this.commandSuggestions.hide();
        }
    }

    private void focusCommandField() {
        if (this.commandField != null) {
            this.setFocused(this.commandField);
            this.commandField.setFocused(true);
            if (this.commandSuggestions != null) {
                this.commandSuggestions.setAllowSuggestions(
                        !this.commandField.getValue().isEmpty());
                this.commandSuggestions.updateCommandInfo();
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.minecraft.isWindowActive()) {
            return false;
        }
        if (this.commandField != null && this.commandField.isFocused()) {
            return this.commandField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.minecraft.isWindowActive()) {
            return false;
        }
        if (this.commandField == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.commandField.isFocused()) {
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
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.onClose();
                return true;
            }
            if (this.commandField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // 输入焦点在终端时，不要把 E/数字键等交给界面快捷键。
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.commandSuggestions != null && this.commandSuggestions.mouseScrolled(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
            ModNetwork.sendToServer(new C2SExecuteCommand(this.source, input.substring(1)));
        } else if (this.minecraft.player != null) {
            // 普通文本走原版聊天；发送后服务端会广播回来，由 ClientChatReceivedEvent 追加到输出区。
            this.minecraft.player.connection.sendChat(input);
        }
    }

    public void appendChat(Component message) {
        if (this.logWidget != null && message != null) {
            this.logWidget.append(message);
        }
    }

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
    // 固定权限的 suggestion provider
    // ------------------------------------------------------------

    /**
     * CommandSuggestions 只用 screen.width / screen.height 来决定补全列表位置。
     * 用一个独立 Screen 承载“补全锚点高度”，避免修改真实界面 height 影响 EMI。
     */
    private static final class SuggestionAnchorScreen extends Screen {
        private SuggestionAnchorScreen(int width, int height) {
            super(Component.empty());
            this.width = width;
            this.height = height;
        }
    }

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

    private static void setSuggestionProvider(ClientPacketListener connection,
                                              ClientSuggestionProvider provider) {
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
