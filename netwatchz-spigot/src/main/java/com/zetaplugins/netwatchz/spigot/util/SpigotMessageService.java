package com.zetaplugins.netwatchz.spigot.util;

import com.zetaplugins.zetacore.services.LocalizationService;
import com.zetaplugins.zetacore.services.MessageService;
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
     * Replaces placeholders first, then converts tags/gradients/hex to legacy codes.
     */
    public static String formatMsg(String msg, Replaceable<?>... replaceables) {
        msg = replacePlaceholders(msg, replaceables);
        return convertToLegacy(msg);
    }

    public String getAndFormatMsg(boolean addPrefix, String path, String fallback, Replaceable<?>... replaceables) {
        if (path.startsWith("messages.")) {
            path = path.substring("messages.".length());
        }

        String msg = this.localizationService.getString(path, fallback);
        String prefix = this.localizationService.getString("prefix", "&8[&aTimberZ&8]");
        msg = !prefix.isEmpty() && addPrefix ? prefix + " " + msg : msg;

        // Apply placeholders (including accent color)
        msg = replacePlaceholdersWithAccentColors(msg, replaceables);

        // Convert to legacy § style string (handles <#HEX>, <gradient:...>, <b> etc.)
        return convertToLegacy(msg);
    }

    public List<String> getAndFormatMsgList(String path, Replaceable<?>... replaceables) {
        if (path.startsWith("messages.")) {
            path = path.substring("messages.".length());
        }

        List<String> msgList = this.localizationService.getStringList(path);
        List<String> components = new ArrayList<>();

        for (String string : msgList) {
            String s = replacePlaceholdersWithAccentColors(string, replaceables);
            components.add(convertToLegacy(s));
        }

        return components;
    }

    public String getAccentColor() {
        // Keep as &-code so legacy translation is possible in config; we convert it later.
        return this.localizationService.getString("accentColor", "&a");
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
     * Convert a (MiniMessage-ish) input to a legacy Spigot string:
     * - <br> -> \n
     * - <#RRGGBB> -> legacy hex §x§R§R§G§G§B§B
     * - <gradient:#RRGGBB:#RRGGBB>text</gradient> -> per-character hex coloring
     * - <b>, <i>, <u>, <s>, <obf>, <!b> etc -> §l, §o, §n, §m, §k, §r
     * - strips <click:...> and <hover:...> tags (keeps inner text)
     *
     * NOTE: This produces a plain String with legacy formatting. Click/Hover events are removed.
     */
    public static String convertToLegacy(String input) {
        if (input == null) return "";

        // First normalize line breaks and remove leading/trailing whitespace artifact
        String out = input.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n");

        // Remove/strip interactive tags but keep their inner content:
        // e.g. <click:copy_to_clipboard:%ip%> ... </click> -> ...
        out = out.replaceAll("(?i)</?click(?::[^>]*)?>", "");
        out = out.replaceAll("(?i)</?hover(?::[^>]*)?>", "");

        // Handle gradients: <gradient:#hex1:#hex2>inner</gradient>
        Pattern gradPat = Pattern.compile("(?i)<gradient:#([0-9a-fA-F]{6}):#([0-9a-fA-F]{6})>(.*?)</gradient>", Pattern.DOTALL);
        Matcher gradMatcher = gradPat.matcher(out);
        StringBuffer gradBuff = new StringBuffer();
        while (gradMatcher.find()) {
            String fromHex = gradMatcher.group(1);
            String toHex = gradMatcher.group(2);
            String inner = gradMatcher.group(3);
            String replaced = applyGradientToText(inner, fromHex, toHex);
            // escape $ for regex replacement safety
            gradMatcher.appendReplacement(gradBuff, Matcher.quoteReplacement(replaced));
        }
        gradMatcher.appendTail(gradBuff);
        out = gradBuff.toString();

        // Replace inline hex tags like <#RRGGBB> with legacy hex sequence
        //out = out.replaceAll("(?i)<#([0-9a-fA-F]{6})>", match -> hexToLegacy(match.group(1)));

        // Basic formatting tags
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

        // Also support shorthand tags used in your original class:
        out = out.replace("<!b>", "§r");
        out = out.replace("<!i>", "§r");
        out = out.replace("<!u>", "§r");
        out = out.replace("<!s>", "§r");
        out = out.replace("<!obf>", "§r");

        // Replace any remaining &-style codes with § (common in config)
        out = out.replace('&', '§');

        return out;
    }

    /**
     * Produce an interpolated color string for each character in text,
     * using legacy §x hex codes before each character.
     */
    private static String applyGradientToText(String text, String fromHex, String toHex) {
        if (text == null || text.isEmpty()) return "";

        int len = text.codePointCount(0, text.length());
        int[] from = hexToRgb(fromHex);
        int[] to = hexToRgb(toHex);

        StringBuilder sb = new StringBuilder();
        // iterate code points to handle non-BMP characters safely
        int cpIndex = 0;
        for (int offset = 0; offset < text.length(); ) {
            int cp = text.codePointAt(offset);
            int charLen = Character.charCount(cp);

            double t = (len == 1) ? 0.0 : ((double) cpIndex) / (len - 1);
            int r = (int) Math.round(lerp(from[0], to[0], t));
            int g = (int) Math.round(lerp(from[1], to[1], t));
            int b = (int) Math.round(lerp(from[2], to[2], t));
            String hex = String.format("%02x%02x%02x", r, g, b);

            sb.append(hexToLegacy(hex));
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
     * Works on Spigot 1.16+ clients.
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
        int index;
        while ((index = builder.indexOf(placeholder)) != -1) {
            builder.replace(index, index + placeholder.length(), replacement);
        }
    }

    static {
        // keep colorMap mapping from &-codes to themselves (we will convert & -> § later)
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

    public static record Replaceable<T>(String placeholder, T value) {
    }
}