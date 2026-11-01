package com.chaoscycle.registry;

import com.chaoscycle.ChaosCycle;
import com.chaoscycle.item.CommandExecutorItem;
import com.chaoscycle.item.HyperspaceLocatorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Constructor;

public final class ModItems {
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChaosCycle.MODID);

    public static final RegistryObject<Item> COMMAND_EXECUTOR = ITEMS.register("command_executor",
            () -> createItem(
                    CommandExecutorItem.class,
                    "com.chaoscycle.item.CurioCommandExecutorItem",
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> HYPERSPACE_LOCATOR = ITEMS.register("hyperspace_locator",
            () -> createItem(
                    HyperspaceLocatorItem.class,
                    "com.chaoscycle.item.CurioHyperspaceLocatorItem",
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /**
     * Curios 是软依赖：没装时只注册基类；装了才通过反射加载实现 ICurioItem 的子类，
     * 避免无 Curios 环境在类加载阶段碰到 ICurioItem 而崩掉。
     */
    private static Item createItem(Class<? extends Item> baseClass, String curioClassName,
                                   Item.Properties properties) {
        if (!CURIOS_LOADED) {
            return newInstance(baseClass, properties);
        }
        try {
            Class<?> curioClass = Class.forName(curioClassName);
            return (Item) curioClass.getConstructor(Item.Properties.class)
                    .newInstance(properties);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // Curios 已安装但加载失败时退回基础物品，保证模组仍然能启动。
            ChaosCycle.LOGGER.warn("Failed to create Curios item {}; falling back to {}",
                    curioClassName, baseClass.getName(), exception);
            return newInstance(baseClass, properties);
        }
    }

    private static Item newInstance(Class<? extends Item> itemClass, Item.Properties properties) {
        try {
            Constructor<? extends Item> constructor = itemClass.getConstructor(Item.Properties.class);
            return constructor.newInstance(properties);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create item " + itemClass.getName(),
                    exception);
        }
    }

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
