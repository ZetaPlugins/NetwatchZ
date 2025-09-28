package com.zetaplugins.netwatchz.spigot.util;

import com.zetaplugins.zetacore.services.LocalizationService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpigotMessageService {
    private final LocalizationService localizationService;
    private static final Map<String, String> colorMap = new HashMap<>();

    public SpigotMessageService(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    /**
     * Format a message for Spigot as a legacy string (with § codes and \n for line breaks).
     */
    public static String formatMsg(String msg, Replaceable<?>... replaceables) {
        msg = replacePlaceholders(msg, replaceables);
        return convertToLegacy(msg);
    }

    public String getAndFormatMsg(boolean addPrefix, String path, String fallback, Replaceable<?>... replaceables) {
        if (path.startsWith("messages.")) {
            path = path.substring("messages.".length());
        }

        String msg = localizationService.getString(path, fallback);
        String prefix = localizationService.getString("prefix", "&8[&aTimberZ&8]");
        msg = !prefix.isEmpty() && addPrefix ? prefix + " " + msg : msg;

        msg = replacePlaceholdersWithAccentColors(msg, replaceables);

        return convertToLegacy(msg);
    }

    public List<String> getAndFormatMsgList(String path, Replaceable<?>... replaceables) {
        if (path.startsWith("messages.")) {
            path = path.substring("messages.".length());
        }

        List<String> msgList = localizationService.getStringList(path);
        List<String> components = new ArrayList<>();

        for (String string : msgList) {
            String s = replacePlaceholdersWithAccentColors(string, replaceables);
            components.add(convertToLegacy(s));
        }

        return components;
    }

    public String getAccentColor() {
        return localizationService.getString("accentColor", "&a");
    }

    public static @NotNull String replacePlaceholders(String msg, Replaceable<?>... replaceables) {
        StringBuilder msgBuilder = new StringBuilder(msg);

        for (Replaceable<?> replaceable : replaceables) {
            String placeholder = replaceable.placeholder();
            String value = String.valueOf(replaceable.value());
            replaceInBuilder(msgBuilder, placeholder, value);
        }

        colorMap.forEach((key, valuex) -> replaceInBuilder(msgBuilder, key, valuex));
        return msgBuilder.toString();
    }

    public @NotNull String replacePlaceholdersWithAccentColors(String msg, Replaceable<?>... replaceables) {
        String replacedMsg = replacePlaceholders(msg, replaceables);
        StringBuilder msgBuilder = new StringBuilder(replacedMsg);
        replaceInBuilder(msgBuilder, "%ac%", this.getAccentColor());
        return msgBuilder.toString();
    }

    /**
     * Convert a MiniMessage input to a legacy Spigot string:
     * - <br> -> \n
     * - <#RRGGBB> -> legacy hex §x§R§R§G§G§B§B
     * - <gradient:#RRGGBB:#RRGGBB>text</gradient> -> per-character hex coloring
     * - <b>, <i>, <u>, <s>, <obf>, <!b> etc -> §l, §o, §n, §m, §k, §r
     * - strips <click:...> and <hover:...> tags (keeps inner text)
     */
    public static String convertToLegacy(String input) {
        if (input == null) return "";

        // replace newline tags with actual newlines
        String out = input.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");

        // remove interactive tags but keep inner text
        out = out.replaceAll("(?i)</?click(?::[^>]*)?>", "");
        out = out.replaceAll("(?i)</?hover(?::[^>]*)?>", "");

        // handle gradients
        Pattern gradPat = Pattern.compile("(?i)<gradient:#([0-9a-fA-F]{6}):#([0-9a-fA-F]{6})>(.*?)</gradient>", Pattern.DOTALL);
        Matcher gradMatcher = gradPat.matcher(out);
        StringBuffer gradBuff = new StringBuffer();
        while (gradMatcher.find()) {
            String fromHex = gradMatcher.group(1);
            String toHex = gradMatcher.group(2);
            String inner = gradMatcher.group(3);
            String replaced = applyGradientToText(inner, fromHex, toHex);
            gradMatcher.appendReplacement(gradBuff, Matcher.quoteReplacement(replaced));
        }
        gradMatcher.appendTail(gradBuff);
        out = gradBuff.toString();

        // Convert inline hex codes <#RRGGBB> to legacy §x format
        Pattern hexPat = Pattern.compile("(?i)<#([0-9a-fA-F]{6})>");
        Matcher hexMatcher = hexPat.matcher(out);
        StringBuffer hexBuff = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(hexBuff, Matcher.quoteReplacement(hexToLegacy(hex)));
        }
        hexMatcher.appendTail(hexBuff);
        out = hexBuff.toString();

        out = out.replace("<b>", "§l");
        out = out.replace("</b>", "§r");
        out = out.replace("<i>", "§o");
        out = out.replace("</i>", "§r");
        out = out.replace("<u>", "§n");
        out = out.replace("</u>", "§r");
        out = out.replace("<s>", "§m");
        out = out.replace("</s>", "§r");
        out = out.replace("<obf>", "§k");
        out = out.replace("</obf>", "§r");

        // just reset all formatting on closing tags (I know it's not perfect but whatever)
        out = out.replace("<!b>", "§r");
        out = out.replace("<!i>", "§r");
        out = out.replace("<!u>", "§r");
        out = out.replace("<!s>", "§r");
        out = out.replace("<!obf>", "§r");

        out = out.replace('&', '§');

        return out;
    }

    private static String applyGradientToText(String text, String fromHex, String toHex) {
        if (text == null || text.isEmpty()) return "";

        text = text.replace('&', '§');

        int len = text.codePointCount(0, text.length());
        int[] from = hexToRgb(fromHex);
        int[] to = hexToRgb(toHex);

        StringBuilder sb = new StringBuilder();
        String activeFormatting = "";

        int cpIndex = 0;
        for (int offset = 0; offset < text.length(); ) {
            int cp = text.codePointAt(offset);
            int charLen = Character.charCount(cp);

            if (cp == '§' && offset + 1 < text.length()) {
                char code = text.charAt(offset + 1);
                if ("lmnok".indexOf(code) != -1) {// Only apply l, m, n, o, k formatting, not colors
                    activeFormatting += "§" + code;
                } else if (code == 'r') {
                    activeFormatting = "";
                }
                offset += 2; // skip § and code
                continue;
            }

            double t = (len == 1) ? 0.0 : ((double) cpIndex) / (len - 1);
            int r = (int) Math.round(lerp(from[0], to[0], t));
            int g = (int) Math.round(lerp(from[1], to[1], t));
            int b = (int) Math.round(lerp(from[2], to[2], t));
            String hex = String.format("%02x%02x%02x", r, g, b);

            sb.append(hexToLegacy(hex));
            sb.append(activeFormatting);
            sb.appendCodePoint(cp);

            offset += charLen;
            cpIndex++;
        }

        return sb.toString();
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int[] hexToRgb(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    /**
     * Convert a hex string RRGGBB to legacy '§x§R§R§G§G§B§B' format.
     */
    private static String hexToLegacy(String hex) {
        hex = hex.replace("#", "");
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toLowerCase().toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    private static void replaceInBuilder(StringBuilder builder, String placeholder, String replacement) {
        if (placeholder == null || placeholder.isEmpty()) return;

        int start = 0;
        while (true) {
            int index = builder.indexOf(placeholder, start);
            if (index == -1) break;
            builder.replace(index, index + placeholder.length(), replacement);
            start = index + replacement.length();
            if (start < 0 || start > builder.length()) break;
        }
    }

    static {
        colorMap.put("&0", "&0");
        colorMap.put("&1", "&1");
        colorMap.put("&2", "&2");
        colorMap.put("&3", "&3");
        colorMap.put("&4", "&4");
        colorMap.put("&5", "&5");
        colorMap.put("&6", "&6");
        colorMap.put("&7", "&7");
        colorMap.put("&8", "&8");
        colorMap.put("&9", "&9");
        colorMap.put("&a", "&a");
        colorMap.put("&b", "&b");
        colorMap.put("&c", "&c");
        colorMap.put("&d", "&d");
        colorMap.put("&e", "&e");
        colorMap.put("&f", "&f");
        colorMap.put("&k", "&k");
        colorMap.put("&l", "&l");
        colorMap.put("&m", "&m");
        colorMap.put("&n", "&n");
        colorMap.put("&o", "&o");
        colorMap.put("&r", "&r");
    }

    public static record Replaceable<T>(String placeholder, T value) {}
}