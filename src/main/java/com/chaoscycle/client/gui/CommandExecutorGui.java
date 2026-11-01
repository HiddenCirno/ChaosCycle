package com.chaoscycle.client.gui;

import com.chaoscycle.ChaosCycle;
import net.minecraft.resources.ResourceLocation;

/**
 * 命令执行器新 GUI 的纹理与图集常量。
 *
 * 纹理 247x209：
 * - 主体 247x167（y=0..166）
 * - 输入区外框 x=6..217, y=7..158；内区 x=7..216, y=8..157（210x150）
 * - 模式按钮四个 22x22，x=218，y=7/33/59/85
 * - 模式状态图集 y=167，四个状态横排（普通/按下/不可用/悬浮）
 * - 页面状态图集 y=189，两个状态横排（普通/当前）
 * 页面按钮在贴图里位于主体下方，但屏幕上实际绘制在主体上方 2px。
 */
public final class CommandExecutorGui {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(ChaosCycle.MODID, "textures/gui/command_executor.png");
    public static final int TEXTURE_WIDTH = 247;
    public static final int TEXTURE_HEIGHT = 209;

    public static final int BODY_WIDTH = 247;
    public static final int BODY_HEIGHT = 167;

    /** 0000FF 标出的输入区外框 / 内区。 */
    public static final int INPUT_OUTER_X = 6;
    public static final int INPUT_OUTER_Y = 7;
    public static final int INPUT_OUTER_WIDTH = 212;
    public static final int INPUT_OUTER_HEIGHT = 152;
    public static final int INPUT_INNER_X = 7;
    public static final int INPUT_INNER_Y = 8;
    public static final int INPUT_INNER_WIDTH = 210;
    public static final int INPUT_INNER_HEIGHT = 150;

    public static final int LINE_HEIGHT = 10;
    /** 输入区最后一行是命令输入行，上面 14 行是日志。 */
    public static final int LOG_LINES = 14;
    public static final int INPUT_LINE_Y = INPUT_INNER_Y + LOG_LINES * LINE_HEIGHT;
    public static final int INPUT_TEXT_X = INPUT_INNER_X + 11;
    public static final int INPUT_TEXT_WIDTH = INPUT_INNER_WIDTH - 13;

    public static final int MODE_BUTTON_SIZE = 22;
    public static final int MODE_X = 218;
    public static final int[] MODE_Y = {7, 33, 59, 85};
    public static final int MODE_ATLAS_Y = 167;
    public static final int MODE_NORMAL_U = 0;
    public static final int MODE_PRESSED_U = 22;
    public static final int MODE_DISABLED_U = 44;
    public static final int MODE_HOVER_U = 66;

    public static final int PAGE_BUTTON_SIZE = 20;
    public static final int PAGE_X = 3;
    public static final int PAGE_STEP = 22;
    /** 贴图里页面按钮在主体下方；实际绘制时镜像到主体上方 2px。 */
    public static final int PAGE_Y = -22;
    public static final int PAGE_ATLAS_Y = 189;
    public static final int PAGE_NORMAL_U = 0;
    public static final int PAGE_SELECTED_U = 20;

    private CommandExecutorGui() {
    }
}
