package com.chaoscycle.client.screen;

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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

public class HyperspaceLocatorScreen extends Screen {
    // 控制在 427×240 的逻辑分辨率内（GUI Scale 2 下的 854×480 窗口）。
    private static final int PANEL_WIDTH = 410;
    private static final int PANEL_HEIGHT = 228;

    private final InteractionHand hand;
    private WaypointListWidget waypointList;
    private EditBox nameField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private Button updateButton;
    private Button teleportButton;
    private Button removeButton;
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
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        double px = player == null ? 0.0D : player.getX();
        double py = player == null ? 64.0D : player.getY();
        double pz = player == null ? 0.0D : player.getZ();

        this.waypointList = this.addRenderableWidget(new WaypointListWidget(
                this.left + 12, this.top + 38, 220, 114, Component.empty()));
        this.waypointList.setOnSelect(index -> loadSelected());

        this.nameField = this.addRenderableWidget(new EditBox(this.font,
                this.left + 244, this.top + 46, 166, 20,
                Component.translatable("gui.chaoscycle.locator.name_hint")));
        this.nameField.setMaxLength(32);
        this.nameField.setHint(Component.translatable("gui.chaoscycle.locator.name_hint"));

        this.xField = this.addRenderableWidget(new EditBox(this.font,
                this.left + 244, this.top + 84, 50, 20, Component.literal("X")));
        this.xField.setMaxLength(32);
        this.xField.setValue(format(px));

        this.yField = this.addRenderableWidget(new EditBox(this.font,
                this.left + 302, this.top + 84, 50, 20, Component.literal("Y")));
        this.yField.setMaxLength(32);
        this.yField.setValue(format(py));

        this.zField = this.addRenderableWidget(new EditBox(this.font,
                this.left + 360, this.top + 84, 50, 20, Component.literal("Z")));
        this.zField.setMaxLength(32);
        this.zField.setValue(format(pz));

        this.addRenderableWidget(Button.builder(Component.translatable("gui.chaoscycle.locator.current"),
                button -> useCurrent()).bounds(this.left + 244, this.top + 112, 76, 20).build());

        this.updateButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.chaoscycle.locator.update"),
                button -> updateSelected()).bounds(this.left + 326, this.top + 112, 84, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.chaoscycle.locator.add_current"),
                button -> addCurrent()).bounds(this.left + 12, this.top + 158, 104, 20).build());

        this.teleportButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.chaoscycle.locator.teleport"),
                button -> teleportSelected()).bounds(this.left + 122, this.top + 158, 84, 20).build());

        this.removeButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.chaoscycle.locator.remove"),
                button -> removeSelected()).bounds(this.left + 212, this.top + 158, 84, 20).build());

        setSelectionButtons(false);
        ModNetwork.sendToServer(new C2SRequestLocatorState(this.hand.ordinal()));
    }

    public void applyState(S2CLocatorState state) {
        if (this.waypointList == null || this.nameField == null || this.xField == null
                || this.yField == null || this.zField == null) {
            return;
        }
        this.waypoints = state.waypoints();
        this.waypointList.setEntries(this.waypoints);

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
        graphics.fill(this.left - 4, this.top - 4,
                this.left + PANEL_WIDTH + 4, this.top + PANEL_HEIGHT + 4, 0xCC101010);
        graphics.fill(this.left - 4, this.top - 4,
                this.left + PANEL_WIDTH + 4, this.top - 3, 0xFF5A5A5A);
        graphics.fill(this.left - 4, this.top + PANEL_HEIGHT + 3,
                this.left + PANEL_WIDTH + 4, this.top + PANEL_HEIGHT + 4, 0xFF5A5A5A);

        graphics.drawString(this.font, this.title, this.left + 12, this.top + 10, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("gui.chaoscycle.locator.list"),
                this.left + 12, this.top + 26, 0xAAAAAA, false);
        graphics.drawString(this.font, Component.translatable("gui.chaoscycle.locator.name"),
                this.left + 244, this.top + 34, 0xAAAAAA, false);
        graphics.drawString(this.font, "X", this.left + 244, this.top + 73, 0xAAAAAA, false);
        graphics.drawString(this.font, "Y", this.left + 302, this.top + 73, 0xAAAAAA, false);
        graphics.drawString(this.font, "Z", this.left + 360, this.top + 73, 0xAAAAAA, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(this.font, this.status, this.left + 12, this.top + 186, 0xFFFF80, false);
        List<net.minecraft.util.FormattedCharSequence> hintLines = this.font.split(
                Component.translatable("gui.chaoscycle.locator.hint"), PANEL_WIDTH - 24);
        int hintY = this.top + 202;
        for (int i = 0; i < Math.min(2, hintLines.size()); i++) {
            graphics.drawString(this.font, hintLines.get(i), this.left + 12, hintY + i * 10,
                    0x888888, false);
        }
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
