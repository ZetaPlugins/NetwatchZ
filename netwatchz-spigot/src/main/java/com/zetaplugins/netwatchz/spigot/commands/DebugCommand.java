package com.zetaplugins.netwatchz.spigot.commands;

import com.zetaplugins.netwatchz.spigot.NetwatchZSpigot;
import com.zetaplugins.netwatchz.spigot.util.SpigotMessageService;
import com.zetaplugins.zetacore.debug.ReportDataCollector;
import com.zetaplugins.zetacore.debug.ReportFileWriter;
import com.zetaplugins.zetacore.debug.data.DebugReport;
import com.zetaplugins.zetacore.debug.uploader.ZetaDebugReportUploader;
import com.zetaplugins.zetacore.services.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * DebugCommandHandler is a command handler for the debug command.
 */
public final class DebugCommand implements CommandExecutor, TabCompleter {
    private final NetwatchZSpigot plugin;
    private final File pluginFile;
    private final String permission = "netwatchz.debug";
    private final String modrinthId = "pVji8xJW";
    private final Map<String, String> configs;

    /**
     * Constructor for DebugCommandHandler.
     * @param plugin the JavaPlugin instance
     */
    public DebugCommand(NetwatchZSpigot plugin) {
        this.plugin = plugin;
        this.pluginFile = plugin.getPluginFile();
        this.configs = Map.of("config.yml", plugin.getConfig().saveToString());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            throwUsageError(sender, plugin.getMessageService().getAndFormatMsg(
                    false,
                    "debug_usage",
                    "&cUsage: /%command% <upload | generate>",
                    new SpigotMessageService.Replaceable<>("%command%", command.getName())
            ));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "upload" -> {
                if (!sender.hasPermission(permission)) {
                    throwPermissionError(sender);
                    yield true;
                }
                yield handleUpload(
                        sender,
                        args.length > 1 && args[1].equalsIgnoreCase("confirm"),
                        command.getName()
                );
            }
            case "generate" -> {
                if (!sender.hasPermission(permission)) {
                    throwPermissionError(sender);
                    yield true;
                }
                yield handleGenerate(sender);
            }
            default -> {
                throwUsageError(sender, plugin.getMessageService().getAndFormatMsg(
                        false,
                        "debug_usage",
                        "&cUsage: /%command% <upload | generate>",
                        new SpigotMessageService.Replaceable<>("%command%", command.getName())
                ));
                yield true;
            }
        };
    }

    private void throwUsageError(@NotNull CommandSender sender, String usage) {
        sender.sendMessage(usage);
    }

    private void throwPermissionError(@NotNull CommandSender sender) {
        sender.sendMessage(
                plugin.getMessageService().getAndFormatMsg(
                        false,
                        "debug_no_permission",
                        "&cYou do not have permission to use this command!"
                )
        );
    }

    private boolean handleUpload(CommandSender sender, boolean confirmed, String commandName) {
        if (!confirmed) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                    false,
                    "debug_upload_confirm",
                    "<br><#8b73f6>&lUploading Debug Report&r<br><br>&7 Are you sure you want to upload the debug report? By confirming, you accept our <u><click:OPEN_URL:https://debug.zetaplugins.com/privacy>Privacy Policy</click></u>.<br><br>&8 <#8b73f6><click:RUN_COMMAND:%command%>[Click Here]</click> &r&8or run <u>%command%</u><br>",
                    new SpigotMessageService.Replaceable<>(
                            "%command%",
                            "/" + commandName + " upload confirm"
                    )
            ));
            return true;
        }

        if (!sender.hasPermission(permission)) {
            throwPermissionError(sender);
            return true;
        }

        DebugReport report = ReportDataCollector.collect(modrinthId, plugin, pluginFile, configs);
        String url = ZetaDebugReportUploader.uploadReport(report, plugin);

        if (url == null) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                    false,
                    "debug_upload_failed",
                    "&cFailed to upload debug report: %error%",
                    new SpigotMessageService.Replaceable<>("%error%", "Failed to upload report.")
            ));
            return false;
        }

        String formattedUrl = url.replaceAll("\\\\", "");

        sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "debug_upload_success",
                "&8 [&a✔&8] &7Debug report uploaded successfully! You can view it here:<br>&8 <u><#8b73f6><click:OPEN_URL:%url%>%url%</click></u><br>",
                new SpigotMessageService.Replaceable<>("%url%", formattedUrl)
        ));
        return true;
    }

    /**
     * Handles the generate command.
     * @param sender the CommandSender who executed the command
     * @return true if the command was handled successfully, false otherwise
     */
    private boolean handleGenerate(CommandSender sender) {
        DebugReport report = ReportDataCollector.collect(modrinthId, plugin, pluginFile, configs);
        File reportJson = new File("debug-report.json");
        File reportTxt = new File("debug-report.txt");

        try {
            ReportFileWriter.writeJsonReportToFile(report, reportJson);
            ReportFileWriter.writeTextReportToFile(report, reportTxt);
        } catch (IOException e) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                    false,
                    "debug_failed_to_create_file",
                    "&cFailed to create debug report file: %error%",
                    new SpigotMessageService.Replaceable<>("%error%", e.getMessage())
            ));
            plugin.getLogger().log(Level.SEVERE, "Failed to write debug report", e);
            return false;
        }

        sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "debug_file_create_success",
                "&8 [&a✔&8] &7Saved debug data to the following files:<br><click:COPY_TO_CLIPBOARD:%jsonPath%><#8b73f6>%jsonPath%</click><br><click:COPY_TO_CLIPBOARD:%txtPath%><#8b73f6>%txtPath%</click>",
                new SpigotMessageService.Replaceable<>("%jsonPath%", reportJson.getAbsolutePath()),
                new SpigotMessageService.Replaceable<>("%txtPath%", reportTxt.getAbsolutePath())
        ));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("upload", "generate");
        }
        return null;
    }
}
