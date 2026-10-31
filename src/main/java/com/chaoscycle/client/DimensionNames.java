package com.chaoscycle.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

/** 维度 ID 的本地化显示：优先 dimension.<namespace>.<path>，没有翻译时退回原始 ID。 */
public final class DimensionNames {
    private DimensionNames() {
    }

    private static String translationKey(String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        return id == null ? null : "dimension." + id.getNamespace() + "." + id.getPath();
    }

    public static String displayName(String dimensionId) {
        String key = translationKey(dimensionId);
        return key != null && I18n.exists(key) ? I18n.get(key) : prettify(dimensionId);
    }

    public static Component component(String dimensionId) {
        String key = translationKey(dimensionId);
        return key != null && I18n.exists(key)
                ? Component.translatable(key)
                : Component.literal(prettify(dimensionId));
    }

    /**
     * 原版 1.20.1 没有 dimension.minecraft.overworld 之类的键；
     * 没有翻译时按 ExplorersCompass 的做法退化：去掉命名空间，下划线换空格，逐词首字母大写。
     * 例如 minecraft:the_nether -> "The Nether"。
     */
    private static String prettify(String dimensionId) {
        String name = dimensionId;
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }
        name = name.replace('_', ' ');
        StringBuilder builder = new StringBuilder(name.length());
        boolean capitalize = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c)) {
                capitalize = true;
                builder.append(c);
            } else {
                builder.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            }
        }
        return builder.toString();
    }

    /**
     * 状态消息里的维度 ID 是以字符串参数塞进 translatable 的；
     * 在客户端把能翻译的参数替换成组件，没有翻译时保留原始 ID。
     */
    public static Component localize(Component message) {
        ComponentContents contents = message.getContents();
        if (!(contents instanceof TranslatableContents translatable)) {
            return message;
        }
        Object[] args = translatable.getArgs();
        Object[] mapped = new Object[args.length];
        boolean changed = false;
        for (int i = 0; i < args.length; i++) {
            // 状态消息里带命名空间的字符串参数就是维度 ID；坐标等参数不含 ':'。
            if (args[i] instanceof String text && text.indexOf(':') > 0) {
                mapped[i] = component(text);
                changed = true;
                continue;
            }
            mapped[i] = args[i];
        }
        if (!changed) {
            return message;
        }
        MutableComponent rebuilt = MutableComponent.create(
                new TranslatableContents(translatable.getKey(), translatable.getFallback(), mapped));
        rebuilt.setStyle(message.getStyle());
        for (Component sibling : message.getSiblings()) {
            rebuilt.append(sibling);
        }
        return rebuilt;
    }
}
