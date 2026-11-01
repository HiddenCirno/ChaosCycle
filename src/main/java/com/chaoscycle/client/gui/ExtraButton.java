package com.chaoscycle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

/**
 * 主面板右侧外挂的 20x20 页面按钮：只有常态/选中两种图。
 * 当前页用选中态；未实现页保持常态且不响应。
 */
public class ExtraButton extends AbstractWidget {
    /**
     * 末地折跃门没有物品形态，只能渲染方块实体。
     * 这个假 BE 只用于 GUI：屏蔽生成/冷却光束，并让六个面都通过剔除。
     */
    private static final TheEndGatewayBlockEntity GATEWAY_ICON = new GatewayIconBlockEntity();

    private final boolean selected;
    private final ItemStack icon;
    private final BlockState blockIcon;
    private final Component tooltip;
    private final Runnable onPress;

    public ExtraButton(int x, int y, boolean selected, ItemStack icon,
                       Component tooltip, Runnable onPress) {
        this(x, y, selected, icon, null, tooltip, onPress);
    }

    public ExtraButton(int x, int y, boolean selected, BlockState blockIcon,
                       Component tooltip, Runnable onPress) {
        this(x, y, selected, ItemStack.EMPTY, blockIcon, tooltip, onPress);
    }

    private ExtraButton(int x, int y, boolean selected, ItemStack icon, BlockState blockIcon,
                        Component tooltip, Runnable onPress) {
        super(x, y, CommandExecutorGui.EXTRA_BUTTON_SIZE, CommandExecutorGui.EXTRA_BUTTON_SIZE, tooltip);
        this.selected = selected;
        this.icon = icon;
        this.blockIcon = blockIcon;
        this.tooltip = tooltip;
        this.onPress = onPress;
    }

    public Component tooltip() {
        return tooltip;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int u = this.selected
                ? CommandExecutorGui.EXTRA_SELECTED_U
                : CommandExecutorGui.EXTRA_NORMAL_U;
        graphics.blit(CommandExecutorGui.TEXTURE, getX(), getY(), u,
                CommandExecutorGui.EXTRA_ATLAS_Y,
                CommandExecutorGui.EXTRA_BUTTON_SIZE, CommandExecutorGui.EXTRA_BUTTON_SIZE,
                CommandExecutorGui.TEXTURE_WIDTH, CommandExecutorGui.TEXTURE_HEIGHT);
        if (!this.icon.isEmpty()) {
            // 16x16 物品图标居中放在 20x20 按钮里。
            graphics.renderItem(this.icon, getX() + 2, getY() + 2);
        } else if (this.blockIcon != null && this.blockIcon.is(Blocks.END_GATEWAY)) {
            renderGatewayIcon(graphics, getX() + 2, getY() + 2);
        }
    }

    /** 把末地折跃门方块以 3D 方块形式渲染进 16x16 的图标区域。 */
    private static void renderGatewayIcon(GuiGraphics graphics, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // 完全复刻原版方块物品的 GUI 变换：
        // GuiGraphics#renderItem: translate(x+8, y+8, 150), scale(1,-1,1), scale(16,16,16)
        // block/block.json 的 gui display: rotation [30,225,0], scale 0.625
        // ItemRenderer#render: translate(-0.5,-0.5,-0.5)
        pose.translate(x + 8.0F, y + 8.0F, 150.0F);
        pose.scale(10.0F, -10.0F, 10.0F);
        pose.mulPose(new Quaternionf().rotationXYZ(
                (float) Math.toRadians(30.0F),
                (float) Math.toRadians(225.0F),
                0.0F));
        pose.translate(-0.5F, -0.5F, -0.5F);
        minecraft.getBlockEntityRenderDispatcher().renderItem(
                GATEWAY_ICON, pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        // renderItem 走的是独立的 RenderType，必须及时 flush，否则会被 GUI 的批次顺序盖住。
        graphics.flush();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !active || !visible || this.selected
                || mouseX < getX() || mouseX >= getX() + width
                || mouseY < getY() || mouseY >= getY() + height) {
            return false;
        }
        // 预留页目前只显示常态，不播放声音也不执行动作。
        this.onPress.run();
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    private static final class GatewayIconBlockEntity extends TheEndGatewayBlockEntity {
        private GatewayIconBlockEntity() {
            super(BlockPos.ZERO, Blocks.END_GATEWAY.defaultBlockState());
        }

        @Override
        public boolean isSpawning() {
            return false;
        }

        @Override
        public boolean isCoolingDown() {
            return false;
        }

        @Override
        public boolean shouldRenderFace(Direction direction) {
            return true;
        }
    }
}
