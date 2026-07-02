package me.totalfreedom.totalfreedommod.command;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.premium")
@CommandParameters(description = "Validates if a given account is premium.", usage = "/<command> <player>", aliases = "prem")
public class Command_premium extends FreedomCommand
{

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        final Player player = getPlayer(args[0]);
        final String name = player == null ? args[0] : player.getName();

        if (!name.matches("^[A-Za-z0-9_]{3,16}$"))
        {
            sender.sendMessage(Component.text(
                    "That is not a valid Minecraft username.",
                    NamedTextColor.RED));
            return true;
        }

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "TotalFreedomMod")
                .GET()
                .build();

        sender.sendMessage(Component.text("Checking the Minecraft account for ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.YELLOW))
                .append(Component.text("...", NamedTextColor.GRAY)));

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) ->
                {
                    if (!plugin.isEnabled())
                    {
                        return;
                    }

                    server.getScheduler().runTask(plugin, () ->
                    {
                        if (throwable != null)
                        {
                            FLog.severe(throwable);
                            sender.sendMessage(Component.text(
                                    "There was an error querying Minecraft Services.",
                                    NamedTextColor.RED));
                            return;
                        }

                        final int statusCode = response.statusCode();

                        if (statusCode == 200)
                        {
                            sender.sendMessage(Component.text("Player ", NamedTextColor.GRAY)
                                    .append(Component.text(name, NamedTextColor.YELLOW))
                                    .append(Component.text(" is premium: ", NamedTextColor.GRAY))
                                    .append(Component.text("Yes", NamedTextColor.DARK_GREEN)));
                            return;
                        }

                        if (statusCode == 204 || statusCode == 404)
                        {
                            sender.sendMessage(Component.text("Player ", NamedTextColor.GRAY)
                                    .append(Component.text(name, NamedTextColor.YELLOW))
                                    .append(Component.text(" is premium: ", NamedTextColor.GRAY))
                                    .append(Component.text("No", NamedTextColor.RED)));
                            return;
                        }

                        if (statusCode == 429)
                        {
                            sender.sendMessage(Component.text(
                                    "Minecraft Services is currently rate limiting requests. Try again shortly.",
                                    NamedTextColor.RED));
                            return;
                        }

                        sender.sendMessage(Component.text(
                                "Minecraft Services returned an unexpected response: HTTP " + statusCode,
                                NamedTextColor.RED));
                    });
                });

        return true;
    }
}