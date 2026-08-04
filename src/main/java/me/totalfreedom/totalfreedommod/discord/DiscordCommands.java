package me.totalfreedom.totalfreedommod.discord;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.sql.adapter.DiscordLinkRepository;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.PlayerListUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * JDA slash command handlers for {@code /list}, {@code /link}, {@code /unlink}.
 */
public class DiscordCommands extends ListenerAdapter
{

    private final TotalFreedomMod plugin;
    private final DiscordBridge bridge;

    public DiscordCommands(TotalFreedomMod plugin, DiscordBridge bridge)
    {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event)
    {
        switch (event.getName())
        {
            case "list" -> handleList(event);
            case "link" -> handleLink(event);
            case "unlink" -> handleUnlink(event);
            default ->
            {
            }
        }
    }

    private void handleList(SlashCommandInteractionEvent event)
    {
        event.deferReply().queue();
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            String body = "```\n" + PlayerListUtil.buildRankList() + "\n```";
            event.getHook().sendMessage(body).queue();
        });
    }

    private void handleLink(SlashCommandInteractionEvent event)
    {
        OptionMapping codeOption = event.getOption("code");
        if (codeOption == null)
        {
            event.reply("Missing `code` argument.").setEphemeral(true).queue();
            return;
        }
        String code = codeOption.getAsString().trim().toUpperCase();
        Optional<UUID> pendingUuid = bridge.consumePendingLink(code);
        if (pendingUuid.isEmpty())
        {
            event.reply("That code is unknown or expired. Run `/link` in-game to get a fresh one.")
                    .setEphemeral(true).queue();
            return;
        }

        UUID adminUuid = pendingUuid.get();
        Admin admin = plugin.al.getAdminByUuid(adminUuid);
        if (admin == null)
        {
            event.reply("Internal error: admin record for the code is gone. Try again.")
                    .setEphemeral(true).queue();
            return;
        }

        DiscordLinkRepository repo = plugin.dm.getDiscordLinkRepository();
        try
        {
            repo.deleteByAdminUuid(adminUuid);
            repo.deleteByDiscordUserId(event.getUser().getId());
            repo.insert(adminUuid, event.getUser().getId());
        }
        catch (SQLException ex)
        {
            FLog.warning("[Discord] /link failed for " + admin.getName() + ": " + ex.getMessage());
            event.reply("Couldn't save the link — see server log. Try again.")
                    .setEphemeral(true).queue();
            return;
        }

        event.reply("Linked as **" + admin.getName() + "** (" + admin.getRank().getName() + ").")
                .setEphemeral(true).queue();
        FLog.info("[Discord] Linked admin " + admin.getName() + " ↔ Discord user " + event.getUser().getId() + ".");
    }

    private void handleUnlink(SlashCommandInteractionEvent event)
    {
        String discordUserId = event.getUser().getId();
        DiscordLinkRepository repo = plugin.dm.getDiscordLinkRepository();
        boolean removed;
        try
        {
            removed = repo.deleteByDiscordUserId(discordUserId);
        }
        catch (SQLException ex)
        {
            FLog.warning("[Discord] /unlink failed for " + discordUserId + ": " + ex.getMessage());
            event.reply("Couldn't remove the link — see server log.")
                    .setEphemeral(true).queue();
            return;
        }
        if (removed)
        {
            event.reply("Link removed.").setEphemeral(true).queue();
            FLog.info("[Discord] Unlinked Discord user " + discordUserId + ".");
        }
        else
        {
            event.reply("You aren't linked.").setEphemeral(true).queue();
        }
    }
}
