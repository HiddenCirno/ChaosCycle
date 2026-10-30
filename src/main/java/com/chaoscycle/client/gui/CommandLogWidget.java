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
 * 聊天栏式的只读输出框：自动换行、贴底显示、滚轮上翻。
 */
public class CommandLogWidget extends AbstractWidget {
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_LINES = 400;
    private static final int COLOR_FRAME = 0xFF6B6B6B;
    private static final int COLOR_BG = 0xFF101010; // 与聊天栏背景同色系
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final List<FormattedCharSequence> lines = new ArrayList<>();
    /** 距离底部的行数；0 = 贴底跟随最新输出。 */
    private int scrolledUp;

    public CommandLogWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void append(Component message) {
        Font font = Minecraft.getInstance().font;
        this.lines.addAll(font.split(message, width - 12));
        if (this.lines.size() > MAX_LINES) {
            this.lines.subList(0, this.lines.size() - MAX_LINES).clear();
        }
        this.scrolledUp = 0;
    }

    public void clear() {
        this.lines.clear();
        this.scrolledUp = 0;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, COLOR_FRAME);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, COLOR_FRAME);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, COLOR_FRAME);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, COLOR_FRAME);

        Font font = Minecraft.getInstance().font;
        int visible = Math.max(1, (height - 4) / LINE_HEIGHT);
        int maxScroll = Math.max(0, lines.size() - visible);
        int scroll = Math.min(scrolledUp, maxScroll);
        int end = lines.size() - scroll;
        int start = Math.max(0, end - visible);

        int y = getY() + height - 3 - LINE_HEIGHT;
        for (int i = end - 1; i >= start; i--) {
            graphics.drawString(font, lines.get(i), getX() + 5, y, COLOR_TEXT, false);
            y -= LINE_HEIGHT;
        }

        // 滚动条：内容超过可视区域时显示。
        if (lines.size() > visible) {
            int barHeight = Math.max(8, (height - 4) * visible / lines.size());
            int barY = getY() + 2 + (height - 4 - barHeight) * (maxScroll - scroll) / Math.max(1, maxScroll);
            graphics.fill(getX() + width - 4, barY, getX() + width - 2, barY + barHeight, 0xFF505050);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < getX() || mouseX >= getX() + width || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        int visible = Math.max(1, (height - 4) / LINE_HEIGHT);
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
