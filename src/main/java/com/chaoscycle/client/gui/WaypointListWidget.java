package com.chaoscycle.client.gui;

import com.chaoscycle.client.DimensionNames;
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
 * 超空间定位器坐标列表：229x107 视口，5 行 x 20px，行距 1px，无滚动条，滚轮翻动。
 * 行背景来自图集（普通/选中/悬浮），文字由代码绘制。
 */
public class WaypointListWidget extends AbstractWidget {
    private static final int COLOR_TEXT = 0xFFE4E4E4;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;
    private static final int COLOR_EMPTY = 0xFF8A8A8A;

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

    /** 删除条目后释放选中，避免后续操作落在下一条上。 */
    public void clearSelection() {
        this.selected = -1;
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
    }

    private int rowY(int visibleRow) {
        return getY() + LocatorGui.ROW_TOP_OFFSET + visibleRow * LocatorGui.ROW_PITCH;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX >= getX() + width) {
            return -1;
        }
        for (int row = 0; row < LocatorGui.VISIBLE_ROWS; row++) {
            int y = rowY(row);
            if (mouseY >= y && mouseY < y + LocatorGui.ROW_HEIGHT) {
                int index = this.scroll + row;
                return index < entries.size() ? index : -1;
            }
        }
        return -1;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (entries.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.translatable("gui.chaoscycle.locator.empty_list"),
                    getX() + 8, getY() + 8, COLOR_EMPTY, false);
            return;
        }

        Font font = Minecraft.getInstance().font;
        for (int row = 0; row < LocatorGui.VISIBLE_ROWS; row++) {
            int index = this.scroll + row;
            if (index >= entries.size()) {
                break;
            }
            int y = rowY(row);
            boolean hovered = mouseX >= getX() && mouseX < getX() + width
                    && mouseY >= y && mouseY < y + LocatorGui.ROW_HEIGHT;
            int v;
            if (index == this.selected) {
                v = LocatorGui.ROW_SELECTED_V;
            } else if (hovered) {
                v = LocatorGui.ROW_HOVER_V;
            } else {
                v = LocatorGui.ROW_NORMAL_V;
            }
            graphics.blit(LocatorGui.TEXTURE, getX(), y, 0, v,
                    LocatorGui.ROW_WIDTH, LocatorGui.ROW_HEIGHT,
                    LocatorGui.TEXTURE_WIDTH, LocatorGui.TEXTURE_HEIGHT);

            WaypointEntry entry = entries.get(index);
            String dimension = DimensionNames.displayName(entry.dimension());
            String text = entry.name().isBlank()
                    ? String.format(Locale.ROOT, "#%d %s @ %.0f, %.0f, %.0f",
                    index + 1, dimension, entry.x(), entry.y(), entry.z())
                    : String.format(Locale.ROOT, "#%d %s · %s",
                    index + 1, entry.name(), dimension);
            graphics.drawString(font, font.plainSubstrByWidth(text, width - 12),
                    getX() + 6, y + 6,
                    index == this.selected ? COLOR_SELECTED : COLOR_TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !active || !visible || entries.isEmpty()) {
            return false;
        }
        int index = rowAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        this.selected = index;
        ensureVisible();
        onSelect.accept(index);
        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (entries.size() <= LocatorGui.VISIBLE_ROWS) {
            return false;
        }
        if (mouseX < getX() || mouseX >= getX() + width
                || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        int direction = delta > 0 ? -1 : 1;
        scroll = Math.max(0, Math.min(entries.size() - LocatorGui.VISIBLE_ROWS, scroll + direction));
        return true;
    }

    private void ensureVisible() {
        if (selected < 0) {
            scroll = 0;
            return;
        }
        if (selected < scroll) {
            scroll = selected;
        } else if (selected >= scroll + LocatorGui.VISIBLE_ROWS) {
            scroll = selected - LocatorGui.VISIBLE_ROWS + 1;
        }
        scroll = Math.max(0, Math.min(scroll,
                Math.max(0, entries.size() - LocatorGui.VISIBLE_ROWS)));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
