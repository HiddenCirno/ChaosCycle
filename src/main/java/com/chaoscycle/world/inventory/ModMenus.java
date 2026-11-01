package com.chaoscycle.world.inventory;

import com.chaoscycle.ChaosCycle;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ChaosCycle.MODID);

    public static final RegistryObject<MenuType<CommandExecutorMenu>> COMMAND_EXECUTOR =
            MENUS.register("command_executor",
                    () -> IForgeMenuType.create(CommandExecutorMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
