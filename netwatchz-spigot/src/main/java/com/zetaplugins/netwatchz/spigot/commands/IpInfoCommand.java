package com.zetaplugins.netwatchz.spigot.commands;

import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.spigot.NetwatchZSpigot;
import com.zetaplugins.netwatchz.spigot.util.SpigotMessageService;
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
    private final NetwatchZSpigot plugin;

    public IpInfoCommand(NetwatchZSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "usage_error",
                "&cUsage: %usage%",
                    new SpigotMessageService.Replaceable<>("%usage%", "/ipinfo <name>")
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

        String playerIp = player.getAddress() != null ? NetwatchZSpigot.getIpFromInetAdress(player.getAddress().getAddress()) : "Unknown IP";

        IpData ipData = plugin.getIpDataFetcher().fetchIpData(playerIp);
        if (ipData == null) {
            sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                    false,
                    "ip_data_fetch_error",
                    "&cFailed to fetch IP data for player %player%!",
                    new SpigotMessageService.Replaceable<>("%player%", playerName)
            ));
            return false;
        }

        sender.sendMessage(plugin.getMessageService().getAndFormatMsg(
                false,
                "ip_info",
                "&7IP: &e%ip%&7 | Country: &e%country%&7 | Region: &e%region%&7 | City: &e%city%&7 | ISP: &e%isp%",
                new SpigotMessageService.Replaceable<>("%player%", playerName),
                new SpigotMessageService.Replaceable<>("%ip%", ipData.ip()),
                new SpigotMessageService.Replaceable<>("%country%", ipData.country() + " (" + ipData.countryCode() + ")"),
                new SpigotMessageService.Replaceable<>("%region%", ipData.regionName() + " (" + ipData.regionCode() + ")"),
                new SpigotMessageService.Replaceable<>("%city%", ipData.city()),
                new SpigotMessageService.Replaceable<>("%isp%", ipData.isp()),
                new SpigotMessageService.Replaceable<>("%org%", ipData.org()),
                new SpigotMessageService.Replaceable<>("%asn%", ipData.asn())
        ));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) return null;
        else return List.of();
    }
}
