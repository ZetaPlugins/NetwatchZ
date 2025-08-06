package com.zetaplugins.netwatchz.paper.commands;

import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.paper.NetwatchZPaper;
import com.zetaplugins.zetacore.services.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IpInfoCommand implements CommandExecutor, TabCompleter {
    private final NetwatchZPaper plugin;

    public IpInfoCommand(NetwatchZPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "usage_error",
                "&cUsage: %usage%",
                new MessageService.Replaceable<>("%usage%", "/ipinfo <name>")
            ));
            return false;
        }

        String playerName = args[0];
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "player_not_found",
                "&cPlayer not found!"
            ));
            return false;
        }

        String playerIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "Unknown IP";

        IpData ipData = plugin.getIpDataFetcher().fetchIpData(playerIp);
        if (ipData == null) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                    false,
                    "ip_data_fetch_error",
                    "&cFailed to fetch IP data for player: %player%",
                    new MessageService.Replaceable<>("%player%", playerName)
            ));
            return false;
        }

        sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "ip_info",
                "&7IP: &e%ip%&7 | Country: &e%country%&7 | Region: &e%region%&7 | City: &e%city%&7 | ISP: &e%isp%",
                new MessageService.Replaceable<>("%player%", playerName),
                new MessageService.Replaceable<>("%ip%", ipData.ip()),
                new MessageService.Replaceable<>("%country%", ipData.country() + " (" + ipData.countryCode() + ")"),
                new MessageService.Replaceable<>("%region%", ipData.regionName() + " (" + ipData.regionCode() + ")"),
                new MessageService.Replaceable<>("%city%", ipData.city()),
                new MessageService.Replaceable<>("%isp%", ipData.isp()),
                new MessageService.Replaceable<>("%org%", ipData.org()),
                new MessageService.Replaceable<>("%asn%", ipData.asn())
        ));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) return null;
        else return List.of();
    }
}
