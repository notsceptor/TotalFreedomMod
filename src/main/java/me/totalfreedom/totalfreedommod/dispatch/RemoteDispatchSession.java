package me.totalfreedom.totalfreedommod.dispatch;

import java.util.Objects;

/**
 * Metadata for an authenticated remote dispatch session (SSH, Discord, ...)
 */
public final class RemoteDispatchSession
{

    public enum Channel
    {
        SSH,
        DISCORD
    }

    private final Channel channel;
    private final String username;
    private final String displayName;
    private final boolean identified;

    public RemoteDispatchSession(Channel channel, String username, String displayName, boolean identified)
    {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.identified = identified;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public String getUsername()
    {
        return username;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Whether the channel proved the user's identity (e.g. SSH public key, a
     * Discord link). Shared-secret channels return false.
     */
    public boolean isIdentified()
    {
        return identified;
    }

    @Override
    public String toString()
    {
        return "RemoteDispatchSession(" + channel + ":" + displayName + ", identified=" + identified + ")";
    }
}
