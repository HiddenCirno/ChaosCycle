package com.chaoscycle.item;

import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 仅在 Curios 已加载时注册的版本。curios:curio 是通用标签，
 * 因此本物品可以被放进任意 Curios 槽位。
 */
public class CurioHyperspaceLocatorItem extends HyperspaceLocatorItem implements ICurioItem {
    public CurioHyperspaceLocatorItem(Item.Properties properties) {
        super(properties);
    }
}
