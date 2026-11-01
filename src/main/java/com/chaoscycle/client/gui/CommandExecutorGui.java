package com.chaoscycle.client.gui;

import com.chaoscycle.ChaosCycle;
import net.minecraft.resources.ResourceLocation;

/**
 * 命令执行器新 GUI 的纹理与图集常量。
 *
 * 纹理 239x229：主面板 199x207，底部是 22x22 模式按钮状态图集，
 * 右上是 20x20 额外按钮状态图集。额外按钮在主面板外 x=201..220。
 */
public final class CommandExecutorGui {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(ChaosCycle.MODID, "textures/gui/command_executor.png");
    public static final int TEXTURE_WIDTH = 239;
    public static final int TEXTURE_HEIGHT = 229;

    public static final int BODY_WIDTH = 199;
    public static final int BODY_HEIGHT = 207;

    public static final int TERMINAL_X = 8;
    public static final int TERMINAL_Y = 7;
    public static final int TERMINAL_WIDTH = 162;
    public static final int TERMINAL_HEIGHT = 112;
    public static final int TERMINAL_INNER_X = 9;
    public static final int TERMINAL_INNER_Y = 8;
    public static final int TERMINAL_INNER_WIDTH = 160;
    public static final int TERMINAL_INNER_HEIGHT = 110;
    public static final int LOG_LINES = 10;
    public static final int LINE_HEIGHT = 10;
    public static final int INPUT_LINE_Y = 108;

    public static final int MODE_BUTTON_SIZE = 22;
    public static final int MODE_X = 170;
    public static final int[] MODE_Y = {7, 31, 55, 79};
    public static final int MODE_ATLAS_Y = 207;
    public static final int MODE_NORMAL_U = 0;
    public static final int MODE_PRESSED_U = 22;
    public static final int MODE_DISABLED_U = 44;
    public static final int MODE_HOVER_U = 66;

    public static final int EXTRA_BUTTON_SIZE = 20;
    public static final int EXTRA_X = 201;
    public static final int[] EXTRA_Y = {8, 32, 56, 80};
    public static final int EXTRA_ATLAS_Y = 0;
    public static final int EXTRA_NORMAL_U = 199;
    public static final int EXTRA_SELECTED_U = 219;

    private CommandExecutorGui() {
    }
}
