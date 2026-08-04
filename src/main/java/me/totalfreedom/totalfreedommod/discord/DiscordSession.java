package me.totalfreedom.totalfreedommod.discord;

import java.util.Optional;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Everything one live gateway connection consists of, as a single immutable value.
 * <p>
 * The bridge publishes this through one volatile reference and never mutates it. That matters
 * because the connection is written on the main thread but read from JDA's gateway threads and
 * from the async console flush task: as separate plain fields there was no happens-before edge
 * between the write and those reads, and a reader could see a channel from one connection
 * alongside a guild from the next. One reference means a reader either sees a whole connection
 * or sees none, and reconnecting is a single assignment rather than five.
 *
 * @param jda              the client this connection belongs to
 * @param guild            configured guild, already resolved
 * @param publicChannel    public chat relay channel, absent when unconfigured or unresolvable
 * @param adminchatChannel admin chat relay channel, absent when unconfigured or unresolvable
 * @param consoleChannel   console relay channel, absent when unconfigured or unresolvable
 */
public record DiscordSession(
        JDA jda,
        Guild guild,
        Optional<TextChannel> publicChannel,
        Optional<TextChannel> adminchatChannel,
        Optional<TextChannel> consoleChannel)
{
    /**
     * Describes the resolved channels for the startup log line.
     */
    public String describeChannels()
    {
        return String.format("public: %s | adminchat: %s | console: %s",
                describe(publicChannel), describe(adminchatChannel), describe(consoleChannel));
    }

    private static String describe(final Optional<TextChannel> channel)
    {
        return channel
                .map(TextChannel::getName)
                .orElse("(none)");
    }
}
