package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.ssh.SshIdentity;
import me.totalfreedom.totalfreedommod.ssh.SshQrServer;
import me.totalfreedom.totalfreedommod.ssh.TotpUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.command.CommandSender;

import java.util.UUID;

@Permission(level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.ssh.totp")
@Command(
        name = "sshtotp",
        description = "Generate a TOTP secret for an SSH identity and serve a one-time QR setup page.",
        usage = "/sshtotp <identity>"
    )
public class Command_sshtotp extends FCommand
{
    /** 
     * It's important to note that identity is the username identity of the player as their file name is set. 
     */
    @Callback
    public void sshtotp(CommandSender sender, String identity)
    {
        if (plugin().sd == null || plugin().sd.getIdentityStore() == null)
        {
            msg(sender, "SSH daemon is not running.");
            return;
        }

        SshIdentity sshIdentity = plugin().sd.getIdentityStore().get(identity);
        if (sshIdentity == null)
        {
            msg(sender, "No SSH identity found for: <identity>", Placeholder.unparsed("identity", identity));
            return;
        }

        String secret = TotpUtil.generateSecret();
        String uri = TotpUtil.buildUri("TotalFreedomMod", identity, secret);
        String token = UUID.randomUUID().toString().replace("-", "");

        plugin().sd.getIdentityStore().setTotpSecret(identity, secret);

        msg(
            sender,
            """
                TOTP secret generated for identity: <identity>
                Secret (keep private): <secret>
                Setup URI: <uri>
                QR page (5 min, one-time): http://<address>:<port>+/qr?t=<token>
            """,
            Placeholder.unparsed("identity", identity),
            Placeholder.unparsed("secret", secret),
            Placeholder.unparsed("uri", uri),
            Placeholder.unparsed("address", getConfigEntry("SERVER_ADDRESS").getString()),
            Formatter.number("port", getConfigEntry("SSH_QR_PORT").getInteger()),
            Placeholder.unparsed("token", token)
        );

        SshQrServer.enqueue(uri, identity, token);
    }
}
