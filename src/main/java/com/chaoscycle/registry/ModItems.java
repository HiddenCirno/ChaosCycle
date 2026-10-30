package com.chaoscycle.registry;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.item.CommandExecutorItem;
import com.chaoscycle.item.HyperspaceLocatorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChaosCycle.MODID);

    public static final RegistryObject<Item> COMMAND_EXECUTOR = ITEMS.register("command_executor",
            () -> new CommandExecutorItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> HYPERSPACE_LOCATOR = ITEMS.register("hyperspace_locator",
            () -> new HyperspaceLocatorItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
