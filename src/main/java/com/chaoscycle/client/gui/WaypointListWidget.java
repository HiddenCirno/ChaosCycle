package com.chaoscycle.client.gui;

import com.chaoscycle.network.WaypointEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * 简易可滚动坐标列表。行高固定，选中行高亮。
 */
public class WaypointListWidget extends AbstractWidget {
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_VISIBLE = 7;
    private static final int COLOR_FRAME = 0xFF6B6B6B;
    private static final int COLOR_BG = 0xFF101010;
    private static final int COLOR_HOVER = 0xFF303030;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_SELECTED = 0xFF55FFFF;
    private static final int COLOR_EMPTY = 0xFF777777;

    private List<WaypointEntry> entries = new ArrayList<>();
    private int selected = -1;
    private int scroll;
    private IntConsumer onSelect = index -> {
    };

    public WaypointListWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setEntries(List<WaypointEntry> entries) {
        this.entries = new ArrayList<>(entries);
        if (this.entries.isEmpty()) {
            this.selected = -1;
            this.scroll = 0;
            return;
        }
        if (this.selected >= this.entries.size()) {
            this.selected = this.entries.size() - 1;
        }
        if (this.selected < 0) {
            this.selected = 0;
        }
        ensureVisible();
    }

    public int getSelectedIndex() {
        return selected;
    }

    public WaypointEntry getSelectedEntry() {
        if (selected < 0 || selected >= entries.size()) {
            return null;
        }
        return entries.get(selected);
    }

    public void setSelected(int index) {
        if (entries.isEmpty()) {
            selected = -1;
            return;
        }
        selected = Math.max(0, Math.min(index, entries.size() - 1));
        ensureVisible();
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        graphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, COLOR_FRAME);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, COLOR_FRAME);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, COLOR_FRAME);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, COLOR_FRAME);

        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.chaoscycle.locator.empty_list"),
                    getX() + 6, getY() + 6, COLOR_EMPTY, false);
            return;
        }

        int visible = Math.min(entries.size(), MAX_VISIBLE);
        for (int row = 0; row < visible; row++) {
            int index = scroll + row;
            if (index >= entries.size()) {
                break;
            }
            int rowY = getY() + 1 + row * ROW_HEIGHT;
            boolean hovered = mouseX >= getX() + 1 && mouseX < getX() + width - 1
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(getX() + 1, rowY, getX() + width - 1, rowY + ROW_HEIGHT, COLOR_HOVER);
            }
            if (index == selected) {
                graphics.fill(getX() + 1, rowY, getX() + 3, rowY + ROW_HEIGHT, COLOR_SELECTED);
            }
            WaypointEntry entry = entries.get(index);
            String text = entry.name().isBlank()
                    ? String.format(Locale.ROOT, "#%d %s @ %.0f, %.0f, %.0f",
                    index + 1, entry.dimension(), entry.x(), entry.y(), entry.z())
                    : String.format(Locale.ROOT, "#%d %s · %s",
                    index + 1, entry.name(), entry.dimension());
            graphics.drawString(font, font.plainSubstrByWidth(text, width - 10),
                    getX() + 5, rowY + 3,
                    index == selected ? COLOR_SELECTED : COLOR_TEXT, false);
        }

        if (entries.size() > MAX_VISIBLE) {
            int barHeight = Math.max(8, (height - 2) * MAX_VISIBLE / entries.size());
            int barY = getY() + 1 + (height - 2 - barHeight) * scroll / Math.max(1, entries.size() - MAX_VISIBLE);
            graphics.fill(getX() + width - 4, barY, getX() + width - 2, barY + barHeight, 0xFF505050);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !active || !visible || entries.isEmpty()) {
            return false;
        }
        if (mouseX < getX() || mouseX >= getX() + width || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        int row = (int) ((mouseY - getY() - 1) / ROW_HEIGHT);
        int index = scroll + row;
        if (index >= 0 && index < entries.size()) {
            selected = index;
            ensureVisible();
            onSelect.accept(index);
            playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (entries.size() <= MAX_VISIBLE) {
            return false;
        }
        if (mouseX < getX() || mouseX >= getX() + width || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        int direction = delta > 0 ? -1 : 1;
        scroll = Math.max(0, Math.min(entries.size() - MAX_VISIBLE, scroll + direction));
        return true;
    }

    private void ensureVisible() {
        if (selected < 0) {
            scroll = 0;
            return;
        }
        if (selected < scroll) {
            scroll = selected;
        } else if (selected >= scroll + MAX_VISIBLE) {
            scroll = selected - MAX_VISIBLE + 1;
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - MAX_VISIBLE)));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
