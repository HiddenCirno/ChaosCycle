package com.chaoscycle.client.screen;

import com.chaoscycle.client.gui.LocatorButton;
import com.chaoscycle.client.gui.LocatorGui;
import com.chaoscycle.client.gui.WaypointListWidget;
import com.chaoscycle.item.HyperspaceLocatorItem;
import com.chaoscycle.network.C2SAddWaypoint;
import com.chaoscycle.network.C2SRemoveWaypoint;
import com.chaoscycle.network.C2SRequestLocatorState;
import com.chaoscycle.network.C2STeleportWaypoint;
import com.chaoscycle.network.C2SUpdateWaypoint;
import com.chaoscycle.network.ModNetwork;
import com.chaoscycle.network.S2CLocatorState;
import com.chaoscycle.network.WaypointEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/**
 * 超空间定位器界面。
 *
 * 全部控件按 424x226 的虚拟坐标系布局，渲染时整体缩放 {@link #SCALE}，
 * 这样可以在不重排贴图坐标的前提下把 GUI 缩小到合适大小。
 */
public class HyperspaceLocatorScreen extends Screen {
    /** 整体显示比例；1.0 等于原始纹理像素，大小交给 MC 自身的 GUI Scale 决定。 */
    private static final float SCALE = 1.0F;

    // 新纹理把列表顶边上移了 1px：视口从 24..130 变成 25..130（106 高）。
    // 5x20 行 + 4x1 间隔，上下各留 1px，视觉上对齐。
    private static final int LIST_X = 9;
    private static final int LIST_Y = 25;
    private static final int LIST_W = 229;
    private static final int LIST_H = 106;
    private static final int NAME_LABEL_X = 245;
    private static final int NAME_LABEL_Y = 27;
    private static final int NAME_X = 245;
    private static final int NAME_Y = 39;
    private static final int NAME_W = 173;
    private static final int NAME_H = 21;
    private static final int COORD_Y = 81;       // 输入框上移 1px 后的位置
    private static final int COORD_W = 56;
    private static final int COORD_H = 19;
    private static final int[] COORD_X = {245, 304, 363};
    private static final int LABEL_Y = 69;
    private static final int TEXT_LEFT = 8;
    private static final int TEXT_TOP = 188;
    private static final int TEXT_WIDTH = 348;   // 右边界与最右侧大按钮对齐（355）

    private final InteractionHand hand;
    private WaypointListWidget waypointList;
    private EditBox nameField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private LocatorButton updateButton;
    private LocatorButton teleportButton;
    private LocatorButton removeButton;
    private Component status = Component.empty();

    private List<WaypointEntry> waypoints = List.of();
    private boolean pendingSelectLast;

    private int left;
    private int top;

    public HyperspaceLocatorScreen(InteractionHand hand) {
        super(Component.translatable("gui.chaoscycle.locator.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        int scaledWidth = Math.round(LocatorGui.BODY_WIDTH * SCALE);
        int scaledHeight = Math.round(LocatorGui.BODY_HEIGHT * SCALE);
        this.left = (this.width - scaledWidth) / 2;
        this.top = (this.height - scaledHeight) / 2;

        Player player = Minecraft.getInstance().player;
        double px = player == null ? 0.0D : player.getX();
        double py = player == null ? 64.0D : player.getY();
        double pz = player == null ? 0.0D : player.getZ();

        this.waypointList = this.addRenderableWidget(new WaypointListWidget(
                LIST_X, LIST_Y, LIST_W, LIST_H, Component.empty()));
        this.waypointList.setOnSelect(index -> loadSelected());

        this.nameField = createField(NAME_X + 4, NAME_Y + 7, NAME_W - 8, 12, 32);

        this.xField = createField(COORD_X[0] + 4, COORD_Y + 6, COORD_W - 8, 12, 32);
        this.xField.setValue(format(px));
        this.yField = createField(COORD_X[1] + 4, COORD_Y + 6, COORD_W - 8, 12, 32);
        this.yField.setValue(format(py));
        this.zField = createField(COORD_X[2] + 4, COORD_Y + 6, COORD_W - 8, 12, 32);
        this.zField.setValue(format(pz));

        this.addRenderableWidget(new LocatorButton(244, 108, false,
                Component.translatable("gui.chaoscycle.locator.current"), this::useCurrent));
        this.updateButton = this.addRenderableWidget(new LocatorButton(335, 108, false,
                Component.translatable("gui.chaoscycle.locator.update"), this::updateSelected));

        this.addRenderableWidget(new LocatorButton(8, 163, true,
                Component.translatable("gui.chaoscycle.locator.add_current"), this::addCurrent));
        this.teleportButton = this.addRenderableWidget(new LocatorButton(127, 163, true,
                Component.translatable("gui.chaoscycle.locator.teleport"), this::teleportSelected));
        this.removeButton = this.addRenderableWidget(new LocatorButton(246, 163, true,
                Component.translatable("gui.chaoscycle.locator.remove"), this::removeSelected));

        setSelectionButtons(false);
        ModNetwork.sendToServer(new C2SRequestLocatorState(this.hand.ordinal()));
    }

    /** 虚拟坐标（面板左上角为 0,0）；无边框 EditBox 覆盖在贴图字段上。 */
    private EditBox createField(int x, int y, int width, int height, int maxLength) {
        EditBox field = this.addRenderableWidget(new EditBox(this.font,
                x, y, width, height, Component.empty()));
        field.setBordered(false);
        field.setMaxLength(maxLength);
        field.setTextColor(0xFFF0F0F0);
        return field;
    }

    private double toVirtualX(double mouseX) {
        return (mouseX - this.left) / SCALE;
    }

    private double toVirtualY(double mouseY) {
        return (mouseY - this.top) / SCALE;
    }

    public void applyState(S2CLocatorState state) {
        if (this.waypointList == null || this.nameField == null || this.xField == null
                || this.yField == null || this.zField == null) {
            return;
        }
        this.waypoints = state.waypoints();
        this.waypointList.setEntries(this.waypoints);

        if (state.clearSelection()) {
            // 删除成功：释放选中，禁用修改/传送/删除，避免连续点击误操作。
            this.waypointList.clearSelection();
            this.pendingSelectLast = false;
            setSelectionButtons(false);
            return;
        }

        if (this.waypoints.isEmpty()) {
            setSelectionButtons(false);
            return;
        }

        if (this.pendingSelectLast) {
            this.pendingSelectLast = false;
            this.waypointList.setSelected(this.waypoints.size() - 1);
        } else if (this.waypointList.getSelectedIndex() < 0) {
            this.waypointList.setSelected(0);
        }
        loadSelected();
    }

    private void loadSelected() {
        WaypointEntry entry = this.waypointList.getSelectedEntry();
        if (entry == null) {
            setSelectionButtons(false);
            return;
        }
        this.nameField.setValue(entry.name());
        this.xField.setValue(format(entry.x()));
        this.yField.setValue(format(entry.y()));
        this.zField.setValue(format(entry.z()));
        setSelectionButtons(true);
    }

    private void setSelectionButtons(boolean enabled) {
        if (this.updateButton != null) {
            this.updateButton.active = enabled;
        }
        if (this.teleportButton != null) {
            this.teleportButton.active = enabled;
        }
        if (this.removeButton != null) {
            this.removeButton.active = enabled;
        }
    }

    private void useCurrent() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        this.xField.setValue(format(player.getX()));
        this.yField.setValue(format(player.getY()));
        this.zField.setValue(format(player.getZ()));
        this.status = Component.translatable("gui.chaoscycle.locator.current_filled");
    }

    private void addCurrent() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        String dimension = currentDimensionId();
        if (dimension.isEmpty()) {
            this.status = Component.translatable("message.chaoscycle.invalid_dimension", "");
            return;
        }
        ModNetwork.sendToServer(new C2SAddWaypoint(this.hand.ordinal(),
                this.nameField.getValue().strip(), dimension,
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()));
        this.pendingSelectLast = true;
        this.status = Component.translatable("message.chaoscycle.saving");
    }

    private void updateSelected() {
        WaypointEntry entry = this.waypointList.getSelectedEntry();
        if (entry == null) {
            this.status = Component.translatable("message.chaoscycle.no_selected_waypoint");
            return;
        }
        Double x = parse(this.xField.getValue());
        Double y = parse(this.yField.getValue());
        Double z = parse(this.zField.getValue());
        if (x == null || y == null || z == null) {
            this.status = Component.translatable("message.chaoscycle.invalid_position");
            return;
        }
        ModNetwork.sendToServer(new C2SUpdateWaypoint(this.hand.ordinal(),
                this.waypointList.getSelectedIndex(), this.nameField.getValue().strip(),
                x, y, z, entry.yaw(), entry.pitch()));
        this.status = Component.translatable("message.chaoscycle.saving");
    }

    private void teleportSelected() {
        WaypointEntry entry = this.waypointList.getSelectedEntry();
        if (entry == null) {
            this.status = Component.translatable("message.chaoscycle.no_selected_waypoint");
            return;
        }
        ModNetwork.sendToServer(new C2STeleportWaypoint(this.hand.ordinal(),
                this.waypointList.getSelectedIndex()));
        this.status = Component.translatable("message.chaoscycle.teleporting");
    }

    private void removeSelected() {
        WaypointEntry entry = this.waypointList.getSelectedEntry();
        if (entry == null) {
            this.status = Component.translatable("message.chaoscycle.no_selected_waypoint");
            return;
        }
        ModNetwork.sendToServer(new C2SRemoveWaypoint(this.hand.ordinal(),
                this.waypointList.getSelectedIndex()));
        this.status = Component.translatable("message.chaoscycle.removing");
    }

    public void setStatus(Component status) {
        this.status = status == null ? Component.empty() : status;
    }

    @Override
    public void tick() {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getItemInHand(this.hand).getItem() instanceof HyperspaceLocatorItem)) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.pose().pushPose();
        graphics.pose().translate(this.left, this.top, 0.0F);
        graphics.pose().scale(SCALE, SCALE, 1.0F);

        graphics.blit(LocatorGui.TEXTURE, 0, 0, 0, 0,
                LocatorGui.BODY_WIDTH, LocatorGui.BODY_HEIGHT,
                LocatorGui.TEXTURE_WIDTH, LocatorGui.TEXTURE_HEIGHT);

        // 除左上角标题外，所有文字都由代码绘制。
        graphics.drawString(this.font, Component.translatable("gui.chaoscycle.locator.name"),
                NAME_LABEL_X, NAME_LABEL_Y, 0xFFD8D8D8, false);
        graphics.drawString(this.font, "X", COORD_X[0], LABEL_Y, 0xFFD8D8D8, false);
        graphics.drawString(this.font, "Y", COORD_X[1], LABEL_Y, 0xFFD8D8D8, false);
        graphics.drawString(this.font, "Z", COORD_X[2], LABEL_Y, 0xFFD8D8D8, false);

        super.render(graphics, (int) toVirtualX(mouseX), (int) toVirtualY(mouseY), partialTick);

        graphics.drawString(this.font, this.status, TEXT_LEFT, TEXT_TOP + 1, 0xFFFF80, false);
        List<FormattedCharSequence> hintLines = this.font.split(
                Component.translatable("gui.chaoscycle.locator.hint"), TEXT_WIDTH);
        for (int i = 0; i < Math.min(2, hintLines.size()); i++) {
            graphics.drawString(this.font, hintLines.get(i), TEXT_LEFT, TEXT_TOP + 14 + i * 10,
                    0xFF909090, false);
        }

        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(toVirtualX(mouseX), toVirtualY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(toVirtualX(mouseX), toVirtualY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(toVirtualX(mouseX), toVirtualY(mouseY), button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(toVirtualX(mouseX), toVirtualY(mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(toVirtualX(mouseX), toVirtualY(mouseY), delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private static String currentDimensionId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.dimension().location().toString();
        }
        return "";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static Double parse(String value) {
        try {
            double parsed = Double.parseDouble(value.strip());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
