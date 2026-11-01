package com.chaoscycle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端日志区：10 行、10px 行高、无滚动条；滚轮只滚动日志。
 * 输入行由 CommandExecutorScreen 单独绘制/编辑，和第 11 行对齐。
 */
public class CommandTerminalWidget extends AbstractWidget {
    private static final int MAX_LINES = 400;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final List<FormattedCharSequence> lines = new ArrayList<>();
    /** 距离底部的行数；0 = 跟随最新输出。 */
    private int scrolledUp;

    public CommandTerminalWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void append(Component message) {
        if (message == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        this.lines.addAll(font.split(message, width - 4));
        if (this.lines.size() > MAX_LINES) {
            this.lines.subList(0, this.lines.size() - MAX_LINES).clear();
        }
        this.scrolledUp = 0;
    }

    public void clear() {
        this.lines.clear();
        this.scrolledUp = 0;
    }

    public List<FormattedCharSequence> snapshotLines() {
        return new ArrayList<>(this.lines);
    }

    public int getScrolledUp() {
        return this.scrolledUp;
    }

    public void restoreSnapshot(List<FormattedCharSequence> snapshot, int scroll) {
        this.lines.clear();
        this.lines.addAll(snapshot);
        this.scrolledUp = Math.max(0, scroll);
    }

    private int visibleLines() {
        return Math.max(1, height / CommandExecutorGui.LINE_HEIGHT);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int visible = visibleLines();
        int maxScroll = Math.max(0, lines.size() - visible);
        int scroll = Math.min(scrolledUp, maxScroll);
        int end = lines.size() - scroll;
        int start = Math.max(0, end - visible);

        int y = getY() + height - CommandExecutorGui.LINE_HEIGHT;
        for (int i = end - 1; i >= start; i--) {
            graphics.drawString(font, lines.get(i), getX() + 2, y + 1, COLOR_TEXT, false);
            y -= CommandExecutorGui.LINE_HEIGHT;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < getX() || mouseX >= getX() + width
                || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        int visible = visibleLines();
        int maxScroll = Math.max(0, lines.size() - visible);
        int direction = delta > 0 ? 1 : -1;
        this.scrolledUp = Math.max(0, Math.min(maxScroll, this.scrolledUp + direction));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
