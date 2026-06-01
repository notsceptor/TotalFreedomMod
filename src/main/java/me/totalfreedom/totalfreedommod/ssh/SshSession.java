package me.totalfreedom.totalfreedommod.ssh;

import java.util.Objects;

/**
 * Metadata for an authenticated SSH session to be carried via command dispatch.
 */
public final class SshSession
{

    private final String username;
    private final String displayName;
    private final boolean publicKeyAuth;

    public SshSession(String username, String displayName, boolean publicKeyAuth)
    {
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.publicKeyAuth = publicKeyAuth;
    }

    public static SshSession create(String username, String prefix, SshAuthMethod authMethod)
    {
        String displayName = (prefix == null ? "" : prefix) + username;
        return new SshSession(username, displayName, authMethod == SshAuthMethod.PUBLIC_KEY);
    }

    public String getUsername()
    {
        return username;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public boolean isPublicKeyAuth()
    {
        return publicKeyAuth;
    }

    @Override
    public String toString()
    {
        return "SshSession(" + displayName + ", pubkey=" + publicKeyAuth + ")";
    }
}
