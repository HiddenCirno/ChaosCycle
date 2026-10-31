package com.chaoscycle.client.gui;

import com.chaoscycle.ChaosCycle;
import net.minecraft.resources.ResourceLocation;

/**
 * 超空间定位器 GUI 的纹理与图集常量。
 * 整张纹理 424x500：上方 424x226 是 GUI 主体，下方是按钮/列表元素状态图集。
 */
public final class LocatorGui {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(ChaosCycle.MODID, "textures/gui/hyperspace_locator.png");
    public static final int TEXTURE_WIDTH = 424;
    public static final int TEXTURE_HEIGHT = 500;

    public static final int BODY_WIDTH = 424;
    public static final int BODY_HEIGHT = 226;

    // 小按钮 84x24 / 大按钮 110x24，三态 + 禁用
    public static final int SMALL_WIDTH = 84;
    public static final int LARGE_WIDTH = 110;
    public static final int BUTTON_HEIGHT = 24;
    public static final int SMALL_U = 0;
    public static final int LARGE_U = 84;
    public static final int BUTTON_NORMAL_V = 226;
    public static final int BUTTON_PRESSED_V = 250;
    public static final int BUTTON_HOVER_V = 274;
    public static final int BUTTON_DISABLED_V = 358;

    // 列表元素 229x20，三态
    public static final int ROW_WIDTH = 229;
    public static final int ROW_HEIGHT = 20;
    public static final int ROW_NORMAL_V = 298;
    public static final int ROW_SELECTED_V = 318;
    public static final int ROW_HOVER_V = 338;
    public static final int ROW_PITCH = 21; // 20px 元素 + 1px 间隔
    public static final int ROW_TOP_OFFSET = 1;
    public static final int VISIBLE_ROWS = 5;

    private LocatorGui() {
    }
}
