package com.zetaplugins.netwatchz.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zetaplugins.netwatchz.common.NetwatchzServices;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.velocity.util.VelocityMessageService;

import java.util.List;

public class IpInfoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final NetwatchzServices services;
    private final VelocityMessageService messageService;

    public IpInfoCommand(ProxyServer server, NetwatchzServices services, VelocityMessageService messageService) {
        this.server = server;
        this.services = services;
        this.messageService = messageService;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length != 1) {
            sender.sendMessage(messageService.getAndFormatMsg(
                    false,
                    "usage_error",
                    "&cUsage: %usage%",
                    new VelocityMessageService.Replaceable<>("%usage%", "/ipinfo <player>")
            ));
            return;
        }

        String targetName = args[0];
        Player player = server.getPlayer(targetName).orElse(null);
        if (player == null) {
            sender.sendMessage(messageService.getAndFormatMsg(
                    false,
                    "player_not_found",
                    "&cPlayer not found!"
            ));
            return;
        }

        String playerIp = player.getRemoteAddress().getAddress().getHostAddress();
//        String playerIp = "89.36.76.135"; // For testing purposes only
        IpData ipData = services.ipDataFetcher().fetchIpData(playerIp);

        if (ipData == null) {
            sender.sendMessage(messageService.getAndFormatMsg(
                    false,
                    "ip_data_fetch_error",
                    "&cFailed to fetch IP data for player %player%!",
                    new VelocityMessageService.Replaceable<>("%player%", targetName)
            ));
            return;
        }

        sender.sendMessage(messageService.getAndFormatMsg(
                false,
                "ip_info",
                "<br>&8--- <gradient:#FF80AB:#D81B60>&lIP Information </gradient>&r&8---<br><br><#FF80AB>Player: <click:copy_to_clipboard:%player%><hover:show_text:'&7Click to copy to clipboard'>&7%player%</hover></click><br><#FF80AB>IP: <click:copy_to_clipboard:%ip%><hover:show_text:'&7Click to copy to clipboard'>&7%ip%</hover></click><br><#FF80AB>Country: <click:copy_to_clipboard:%country%><hover:show_text:'&7Click to copy to clipboard'>&7%country%</hover></click><br><#FF80AB>Region: <click:copy_to_clipboard:%region%><hover:show_text:'&7Click to copy to clipboard'>&7%region%</hover></click><br><#FF80AB>ISP: <click:copy_to_clipboard:%isp%><hover:show_text:'&7Click to copy to clipboard'>&7%isp%</hover></click><br><#FF80AB>Organization: <click:copy_to_clipboard:%org%><hover:show_text:'&7Click to copy to clipboard'>&7%org%</hover></click><br><#FF80AB>ASN: <click:copy_to_clipboard:%asn%><hover:show_text:'&7Click to copy to clipboard'>&7%asn%</hover></click><br><br>&8----------------------<br>",
                new VelocityMessageService.Replaceable<>("%player%", targetName),
                new VelocityMessageService.Replaceable<>("%ip%", ipData.ip()),
                new VelocityMessageService.Replaceable<>("%country%", ipData.country() + " (" + ipData.countryCode() + ")"),
                new VelocityMessageService.Replaceable<>("%region%", ipData.regionName() + " (" + ipData.regionCode() + ")"),
                new VelocityMessageService.Replaceable<>("%city%", ipData.city()),
                new VelocityMessageService.Replaceable<>("%isp%", ipData.isp()),
                new VelocityMessageService.Replaceable<>("%org%", ipData.org()),
                new VelocityMessageService.Replaceable<>("%asn%", ipData.asn())
        ));
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
