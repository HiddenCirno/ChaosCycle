package com.chaoscycle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 使用超空间定位器图集的小/大按钮：普通、按下、悬浮、禁用四种状态。
 * 文本由代码居中绘制。
 */
public class LocatorButton extends AbstractWidget {
    private final boolean large;
    private final Runnable onPress;
    private boolean pressed;

    public LocatorButton(int x, int y, boolean large, Component message, Runnable onPress) {
        super(x, y, large ? LocatorGui.LARGE_WIDTH : LocatorGui.SMALL_WIDTH,
                LocatorGui.BUTTON_HEIGHT, message);
        this.large = large;
        this.onPress = onPress;
    }

    private boolean isOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int u = this.large ? LocatorGui.LARGE_U : LocatorGui.SMALL_U;
        int v;
        if (!this.active) {
            v = LocatorGui.BUTTON_DISABLED_V;
        } else if (this.pressed && isOver(mouseX, mouseY)) {
            v = LocatorGui.BUTTON_PRESSED_V;
        } else if (isOver(mouseX, mouseY)) {
            v = LocatorGui.BUTTON_HOVER_V;
        } else {
            v = LocatorGui.BUTTON_NORMAL_V;
        }
        int w = this.large ? LocatorGui.LARGE_WIDTH : LocatorGui.SMALL_WIDTH;
        graphics.blit(LocatorGui.TEXTURE, getX(), getY(), u, v, w, LocatorGui.BUTTON_HEIGHT,
                LocatorGui.TEXTURE_WIDTH, LocatorGui.TEXTURE_HEIGHT);
        int color = this.active ? 0xFFFFFFFF : 0xFF8A8A8A;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + w / 2, getY() + (LocatorGui.BUTTON_HEIGHT - 8) / 2 + 1, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0 || !isOver(mouseX, mouseY)) {
            return false;
        }
        this.pressed = true;
        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.pressed) {
            return false;
        }
        this.pressed = false;
        if (this.active && isOver(mouseX, mouseY)) {
            this.onPress.run();
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
