package com.zetaplugins.netwatchz.velocity.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VelocityMessageService {

    private final VelocityLocalizationService localizationService;
    private final Map<String, String> colorMap;

    public VelocityMessageService(VelocityLocalizationService localizationService) {
        this.localizationService = localizationService;

        this.colorMap = new HashMap<>();
        this.colorMap.put("&0", "<black>");
        this.colorMap.put("&1", "<dark_blue>");
        this.colorMap.put("&2", "<dark_green>");
        this.colorMap.put("&3", "<dark_aqua>");
        this.colorMap.put("&4", "<dark_red>");
        this.colorMap.put("&5", "<dark_purple>");
        this.colorMap.put("&6", "<gold>");
        this.colorMap.put("&7", "<gray>");
        this.colorMap.put("&8", "<dark_gray>");
        this.colorMap.put("&9", "<blue>");
        this.colorMap.put("&a", "<green>");
        this.colorMap.put("&b", "<aqua>");
        this.colorMap.put("&c", "<red>");
        this.colorMap.put("&d", "<light_purple>");
        this.colorMap.put("&e", "<yellow>");
        this.colorMap.put("&f", "<white>");
        this.colorMap.put("&k", "<obfuscated>");
        this.colorMap.put("&l", "<bold>");
        this.colorMap.put("&m", "<strikethrough>");
        this.colorMap.put("&n", "<underlined>");
        this.colorMap.put("&o", "<italic>");
        this.colorMap.put("&r", "<reset>");
    }

    protected VelocityLocalizationService getLocalizationService() {
        return localizationService;
    }

    public Component formatMsg(String msg, Replaceable<?>... replaceables) {
        msg = replacePlaceholders(msg, replaceables);
        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize("<!i>" + msg);
    }

    public Component getAndFormatMsg(boolean addPrefix, String path, String fallback, Replaceable<?>... replaceables) {
        if (path.startsWith("messages.")) {
            path = path.substring("messages.".length());
        }

        MiniMessage mm = MiniMessage.miniMessage();
        String msg = "<!i>" + localizationService.getString(path, fallback);

        String prefix = localizationService.getString("prefix", "&8[<gradient:#FF80AB:#D81B60>NetwatchZ&8]");
        if (addPrefix && !prefix.isEmpty()) {
            msg = prefix + " " + msg;
        }

        msg = replacePlaceholdersWithAccentColors(msg, replaceables);
        return mm.deserialize(msg);
    }

    public List<Component> getAndFormatMsgList(String path, Replaceable<?>... replaceables) {
        if (path.startsWith("messages.")) {
            path = path.substring("messages.".length());
        }

        MiniMessage mm = MiniMessage.miniMessage();
        List<String> msgList = localizationService.getStringList(path);
        List<Component> components = new ArrayList<>();

        for (String string : msgList) {
            String msg = "<!i>" + string;
            msg = replacePlaceholdersWithAccentColors(msg, replaceables);
            components.add(mm.deserialize(msg));
        }

        return components;
    }

    public String getAccentColor() {
        return localizationService.getString("accentColor", "<#00D26A>");
    }

    public @NotNull String replacePlaceholders(String msg, Replaceable<?>... replaceables) {
        StringBuilder msgBuilder = new StringBuilder(msg);

        for (Replaceable<?> replaceable : replaceables) {
            String placeholder = replaceable.placeholder();
            String value = String.valueOf(replaceable.value());
            replaceInBuilder(msgBuilder, placeholder, value);
        }

        colorMap.forEach((key, value) -> replaceInBuilder(msgBuilder, key, value));
        return msgBuilder.toString();
    }

    public @NotNull String replacePlaceholdersWithAccentColors(String msg, Replaceable<?>... replaceables) {
        String replacedMsg = replacePlaceholders(msg, replaceables);
        StringBuilder msgBuilder = new StringBuilder(replacedMsg);
        replaceInBuilder(msgBuilder, "%ac%", getAccentColor());
        return msgBuilder.toString();
    }

    protected void replaceInBuilder(StringBuilder builder, String placeholder, String replacement) {
        int index;
        while ((index = builder.indexOf(placeholder)) != -1) {
            builder.replace(index, index + placeholder.length(), replacement);
        }
    }

    public static record Replaceable<T>(String placeholder, T value) {
    }
}