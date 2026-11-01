package com.chaoscycle.world.inventory;

import com.chaoscycle.item.CommandExecutorItem;
import com.chaoscycle.util.ItemSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 命令执行器的容器菜单。
 *
 * 贴图上的槽位是 18x18 外框；Slot 的 x/y 指向内部 16x16 物品区，
 * 所以所有坐标都从设计图 +1 内缩。
 */
public class CommandExecutorMenu extends AbstractContainerMenu {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final Inventory playerInventory;
    private final Player player;
    private final ItemSource source;

    /** 服务端构造：菜单来源由打开它的代码直接给出。 */
    public CommandExecutorMenu(int containerId, Inventory playerInventory, ItemSource source) {
        super(ModMenus.COMMAND_EXECUTOR.get(), containerId);
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.source = source;
        addSlots();
    }

    /** 客户端构造：IForgeMenuType 从 openScreen 的附加数据里读来源。 */
    public CommandExecutorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, ItemSource.decode(data));
    }

    private void addSlots() {
        // 玩家背包 9x3：图集槽位起点 (8,123)，Slot 内部 +1。
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col,
                        9 + col * 18, 124 + row * 18));
            }
        }

        // 快捷栏 9x1：图集槽位起点 (8,181)，Slot 内部 +1。
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 9 + col * 18, 182));
        }

        // 装备栏：头、胸、腿、脚，索引 39/38/37/36。
        for (int row = 0; row < 4; row++) {
            EquipmentSlot equipment = ARMOR_SLOTS[row];
            this.addSlot(new Slot(playerInventory, 39 - row, 175, 106 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(equipment, CommandExecutorMenu.this.player)
                            && super.mayPlace(stack);
                }
            });
        }

        // 副手。
        this.addSlot(new Slot(playerInventory, 40, 175, 182));
    }

    public ItemSource getSource() {
        return source;
    }

    /** 旁观者可以打开界面查看，但所有槽位点击/Shift 操作都必须被丢弃。 */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player.isSpectator()) {
            // 原版 handleContainerClick 已经会拦截；这里再兜一层，
            // 防止以后自定义的容器操作绕过原版路径。
            this.sendAllDataToRemote();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.isSpectator()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 36) {
            // 背包 -> 快捷栏（36..44）。装备/副手只接受玩家手动放入。
            if (!this.moveItemStackTo(stack, 36, 45, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 快捷栏/装备/副手 -> 背包。
            if (!this.moveItemStackTo(stack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player
                && this.source.matches(player, CommandExecutorItem.class);
    }
}
