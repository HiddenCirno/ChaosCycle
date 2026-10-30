package com.chaoscycle.client.screen;

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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 聊天栏式的命令执行器界面。
 *
 * 输入部分刻意复用原版聊天框的行为：
 * EditBox（无边框）+ 原版 CommandSuggestions + 原版最近消息历史。
 *
 * CommandSuggestions 的 Y 坐标是按原版聊天栏硬编码的
 * （anchorToBottom ? screen.height - 12 : 72），所以这里在布局完成后把
 * Screen.height 换成一个假值，让补全列表正好落在输入行上方；
 * 渲染背景时再临时恢复真实高度。补全的 X 仍然取自 EditBox 的真实坐标。
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
    private static final int INPUT_BOX_Y = 206; // 输入行背景顶边，也是补全列表的底边
    // 只保留生存 / 创造：旁观模式没有物品栏、右键也无 Item#use，
    // 误切进去后不容易回来，调试道具不做这个入口。
    private static final GameType[] GAME_MODES = {
            GameType.SURVIVAL, GameType.CREATIVE
    };

    private final InteractionHand hand;
    private final Button[] gameModeButtons = new Button[GAME_MODES.length];
    private GameType gameMode = GameType.SURVIVAL;
    private EditBox commandField;
    private CommandSuggestions commandSuggestions;
    private CommandLogWidget logWidget;

    private String historyBuffer = "";
    private int historyPos;
    private int realHeight;

    private int left;
    private int top;

    public CommandExecutorScreen(InteractionHand hand) {
        super(Component.translatable("gui.chaoscycle.command_executor.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        // 进入 init 时 height 仍是真实值（Screen#init / Screen#resize 都会先写入真实尺寸）。
        this.realHeight = this.height;
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
        this.logWidget.append(Component.translatable("gui.chaoscycle.command_executor.hint")
                .withStyle(ChatFormatting.DARK_GRAY));

        // CommandSuggestions 的 Tab 补全要求 screen.getFocused() == input。
        this.setFocused(this.commandField);

        // 让原版补全列表的底边正好落在输入行上方：
        // showSuggestions 使用的 Y = screen.height - 12，SuggestionsList 再上移 3 + 行高。
        this.height = this.top + INPUT_BOX_Y + 15;

        // commandsOnly=true：输入框里的内容始终按指令解析，不要求前导斜杠。
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.commandField,
                this.font, true, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();

        List<String> recent = this.minecraft.gui.getChat().getRecentChat();
        this.historyPos = recent.size();
        this.historyBuffer = "";

        refreshGameModeButtons();
        ModNetwork.sendToServer(new C2SRequestExecutorState(this.hand.ordinal()));
        // 补全使用的命令树也要切到 2 级权限，关闭界面时再恢复玩家自己的命令树。
        ModNetwork.sendToServer(new C2SCommandTreeRequest(this.hand.ordinal(), true));
    }

    @Override
    public void removed() {
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
        String command = this.commandField.getValue().strip();
        if (command.isEmpty()) {
            if (this.logWidget != null) {
                this.logWidget.append(Component.translatable("message.chaoscycle.command.empty")
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (this.logWidget != null) {
            String shown = command.startsWith("/") ? command : "/" + command;
            this.logWidget.append(Component.literal("> " + shown).withStyle(ChatFormatting.GRAY));
        }
        this.minecraft.gui.getChat().addRecentChat(command);
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        this.historyBuffer = "";
        // 与原版聊天一致：执行后清空输入。
        this.commandField.setValue("");
        ModNetwork.sendToServer(new C2SExecuteCommand(this.hand.ordinal(), command));
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

        // 补全/用法提示最后画，盖在输出区与输入行之上。
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
}
