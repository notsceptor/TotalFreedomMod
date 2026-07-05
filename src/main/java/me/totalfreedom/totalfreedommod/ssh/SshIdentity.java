package me.totalfreedom.totalfreedommod.ssh;

import java.util.Map;

public record SshIdentity(
        String identifier,
        String lastLogin,
        String rank,
        String totpSecret,
        Map<String, String> keys)
{
}
