package com.chaoscycle.client.gui;

import com.chaoscycle.client.DimensionNames;
import com.chaoscycle.network.WaypointEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * 超空间定位器坐标列表：229x107 视口，5 行 x 20px，行距 1px，无滚动条，滚轮翻动。
 * 行背景来自图集（普通/选中/悬浮），文字由代码绘制。
 *
 * 列表项支持按住拖动排序：拖动过程中只在客户端本地重排，
 * 松手时通过 onReorder 把原始位置和目标位置发给服务端落盘。
 */
public class WaypointListWidget extends AbstractWidget {
    private static final int COLOR_TEXT = 0xFFE4E4E4;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;
    private static final int COLOR_EMPTY = 0xFF8A8A8A;
    private static final double DRAG_THRESHOLD = 3.0D;

    private List<WaypointEntry> entries = new ArrayList<>();
    private int selected = -1;
    private int scroll;
    private IntConsumer onSelect = index -> {
    };
    private BiConsumer<Integer, Integer> onReorder = (from, to) -> {
    };

    private boolean dragging;
    private boolean dragMoved;
    private int dragIndex = -1;
    private int dragStartIndex = -1;
    private double dragStartMouseY;
    private double dragMouseY;

    public WaypointListWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public void setEntries(List<WaypointEntry> entries) {
        cancelDrag();
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
        cancelDrag();
        if (entries.isEmpty()) {
            selected = -1;
            return;
        }
        selected = Math.max(0, Math.min(index, entries.size() - 1));
        ensureVisible();
    }

    /** 删除条目后释放选中，避免后续操作落在下一条上。 */
    public void clearSelection() {
        cancelDrag();
        this.selected = -1;
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
    }

    /** 拖拽结束且位置确实变化时调用：(原索引, 新索引)。 */
    public void setOnReorder(BiConsumer<Integer, Integer> onReorder) {
        this.onReorder = onReorder;
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

    /** 拖动目标位置：按行中心计算，列表外则夹到可见范围内的第一条/最后一条。 */
    private int dragTargetAt(double mouseY) {
        if (entries.isEmpty()) {
            return -1;
        }
        int row = Mth.floor((mouseY - (getY() + LocatorGui.ROW_TOP_OFFSET))
                / (double) LocatorGui.ROW_PITCH);
        row = Mth.clamp(row, 0, LocatorGui.VISIBLE_ROWS - 1);
        return Mth.clamp(this.scroll + row, 0, entries.size() - 1);
    }

    private void moveDraggedTo(int target) {
        if (target < 0 || target >= entries.size() || target == dragIndex) {
            return;
        }
        WaypointEntry entry = entries.remove(dragIndex);
        entries.add(target, entry);
        dragIndex = target;
        selected = target;
    }

    private void cancelDrag() {
        dragging = false;
        dragMoved = false;
        dragIndex = -1;
        dragStartIndex = -1;
        dragStartMouseY = 0.0D;
        dragMouseY = 0.0D;
    }

    /** 由界面每 tick 调用：拖到列表上下边缘时自动滚动，方便跨屏排序。 */
    public void tick() {
        if (!dragging || !dragMoved || entries.size() <= LocatorGui.VISIBLE_ROWS) {
            return;
        }
        int maxScroll = Math.max(0, entries.size() - LocatorGui.VISIBLE_ROWS);
        int oldScroll = scroll;
        int topEdge = getY() + LocatorGui.ROW_TOP_OFFSET + 2;
        int bottomEdge = getY() + height - 2;
        if (dragMouseY < topEdge && scroll > 0) {
            scroll--;
        } else if (dragMouseY > bottomEdge && scroll < maxScroll) {
            scroll++;
        }
        if (scroll != oldScroll) {
            moveDraggedTo(dragTargetAt(dragMouseY));
        }
    }

    private String textFor(WaypointEntry entry, int index) {
        String dimension = DimensionNames.displayName(entry.dimension());
        return entry.name().isBlank()
                ? String.format(Locale.ROOT, "#%d %s @ %.0f, %.0f, %.0f",
                index + 1, dimension, entry.x(), entry.y(), entry.z())
                : String.format(Locale.ROOT, "#%d %s · %s",
                index + 1, entry.name(), dimension);
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

            if (dragging && dragMoved && index == dragIndex) {
                // 拖动中的条目由 renderFloatingRow 跟随鼠标绘制，
                // 这里只留一个淡化的占位，标出松手后的落点。
                graphics.setColor(1.0F, 1.0F, 1.0F, 0.25F);
                graphics.blit(LocatorGui.TEXTURE, getX(), y, 0, LocatorGui.ROW_SELECTED_V,
                        LocatorGui.ROW_WIDTH, LocatorGui.ROW_HEIGHT,
                        LocatorGui.TEXTURE_WIDTH, LocatorGui.TEXTURE_HEIGHT);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                continue;
            }

            boolean hovered = !dragging && mouseX >= getX() && mouseX < getX() + width
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
            graphics.drawString(font, font.plainSubstrByWidth(textFor(entry, index), width - 12),
                    getX() + 6, y + 6,
                    index == this.selected ? COLOR_SELECTED : COLOR_TEXT, false);
        }

        if (dragging && dragMoved) {
            renderFloatingRow(graphics, font);
        }
    }

    private void renderFloatingRow(GuiGraphics graphics, Font font) {
        if (entries.isEmpty() || dragIndex < 0 || dragIndex >= entries.size()) {
            return;
        }
        int y = Mth.clamp(Mth.floor(dragMouseY - LocatorGui.ROW_HEIGHT / 2.0D),
                getY() + LocatorGui.ROW_TOP_OFFSET,
                getY() + height - LocatorGui.ROW_HEIGHT);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.blit(LocatorGui.TEXTURE, getX(), y, 0, LocatorGui.ROW_SELECTED_V,
                LocatorGui.ROW_WIDTH, LocatorGui.ROW_HEIGHT,
                LocatorGui.TEXTURE_WIDTH, LocatorGui.TEXTURE_HEIGHT);
        WaypointEntry entry = entries.get(dragIndex);
        graphics.drawString(font, font.plainSubstrByWidth(textFor(entry, dragIndex), width - 12),
                getX() + 6, y + 6, COLOR_SELECTED, false);
        graphics.pose().popPose();
    }

    /**
     * 拖动期间始终把自己算作鼠标下的控件：原版的 ContainerEventHandler#mouseReleased
     * 只会把松开事件派发给鼠标当前位置的控件；不这样做的话，拖到列表外松手就不会收到
     * mouseReleased，拖拽状态会卡住，排序也不会提交。
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dragging || super.isMouseOver(mouseX, mouseY);
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
        cancelDrag();
        this.selected = index;
        ensureVisible();
        onSelect.accept(index);

        this.dragging = true;
        this.dragMoved = false;
        this.dragIndex = index;
        this.dragStartIndex = index;
        this.dragStartMouseY = mouseY;
        this.dragMouseY = mouseY;

        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !dragging) {
            return false;
        }
        this.dragMouseY = mouseY;
        if (!dragMoved && Math.abs(mouseY - dragStartMouseY) >= DRAG_THRESHOLD) {
            dragMoved = true;
        }
        if (dragMoved) {
            moveDraggedTo(dragTargetAt(mouseY));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !dragging) {
            return false;
        }
        int from = dragStartIndex;
        int to = dragIndex;
        boolean reordered = dragMoved && from >= 0 && to >= 0 && from != to;
        cancelDrag();
        if (reordered) {
            onReorder.accept(from, to);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (dragging || entries.size() <= LocatorGui.VISIBLE_ROWS) {
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
