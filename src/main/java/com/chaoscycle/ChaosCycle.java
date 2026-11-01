package com.chaoscycle;

import com.chaoscycle.network.ModNetwork;
import com.chaoscycle.registry.ModItems;
import com.chaoscycle.util.LocatorData;
import com.chaoscycle.world.inventory.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ChaosCycle.MODID)
public final class ChaosCycle {
    public static final String MODID = "chaoscycle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChaosCycle() {
        // Forge 47.3.x 不支持构造器注入，这里统一用 FMLJavaModLoadingContext，兼容 47.x 全系。
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModNetwork.register();
        modBus.addListener(this::addCreativeTabContents);
        MinecraftForge.EVENT_BUS.addListener(LocatorData::onPlayerClone);

        LOGGER.info("Chaos Cycle loaded: debug command executor + hyperspace locator");
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(new ItemStack(ModItems.COMMAND_EXECUTOR.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(new ItemStack(ModItems.HYPERSPACE_LOCATOR.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
