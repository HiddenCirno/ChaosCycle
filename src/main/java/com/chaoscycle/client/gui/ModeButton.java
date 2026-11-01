package com.chaoscycle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.function.Consumer;

/**
 * 22x22 模式按钮。底图来自 y=167 的状态图集，图标用物品渲染。
 * 状态顺序：普通 / 按下 / 不可用 / 悬浮；当前模式不做“选中”外观。
 */
public class ModeButton extends AbstractWidget {
    private final GameType mode;
    private final ItemStack icon;
    private final Component tooltip;
    private final Consumer<GameType> onPress;
    private boolean pressed;

    public ModeButton(int x, int y, GameType mode, ItemStack icon, Consumer<GameType> onPress) {
        super(x, y, CommandExecutorGui.MODE_BUTTON_SIZE, CommandExecutorGui.MODE_BUTTON_SIZE,
                Component.translatable("gui.chaoscycle.gamemode." + mode.getName()));
        this.mode = mode;
        this.icon = icon;
        this.tooltip = Component.translatable("gui.chaoscycle.gamemode." + mode.getName());
        this.onPress = onPress;
    }

    public Component tooltip() {
        return tooltip;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int u;
        if (!this.active) {
            u = CommandExecutorGui.MODE_DISABLED_U;
        } else if (this.pressed) {
            u = CommandExecutorGui.MODE_PRESSED_U;
        } else if (this.isHovered) {
            u = CommandExecutorGui.MODE_HOVER_U;
        } else {
            u = CommandExecutorGui.MODE_NORMAL_U;
        }
        graphics.blit(CommandExecutorGui.TEXTURE, getX(), getY(), u,
                CommandExecutorGui.MODE_ATLAS_Y,
                CommandExecutorGui.MODE_BUTTON_SIZE, CommandExecutorGui.MODE_BUTTON_SIZE,
                CommandExecutorGui.TEXTURE_WIDTH, CommandExecutorGui.TEXTURE_HEIGHT);
        graphics.renderItem(this.icon, getX() + 3, getY() + 3);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.pressed || super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Screen.mouseClicked 会把事件依次传给所有子控件，必须自己判断是否落在按钮内；
        // 否则第一个按钮会吞掉整个界面的点击。
        if (button != 0 || !active || !visible
                || mouseX < getX() || mouseX >= getX() + width
                || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        this.pressed = true;
        playDownSound(Minecraft.getInstance().getSoundManager());
        // 使用和原版 F3+F4 相同的 gamemode 指令；由命令执行器固定 2 级权限执行。
        this.onPress.accept(this.mode);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.pressed) {
            this.pressed = false;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
