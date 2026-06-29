package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.ssh.SshIdentity;
import me.totalfreedom.totalfreedommod.ssh.SshQrServer;
import me.totalfreedom.totalfreedommod.ssh.TotpUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.ssh.totp")
@CommandParameters(
        description = "Generate a TOTP secret for an SSH identity and serve a one-time QR setup page.",
        usage = "/<command> <identity> [port]")
public class Command_sshtotp extends FreedomCommand
{
    private static final int DEFAULT_PORT = 8765;

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length < 1)
        {
            return false;
        }

        String identifier = args[0];
        int port = DEFAULT_PORT;

        if (args.length >= 2)
        {
            try
            {
                port = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                msg("Invalid port: " + args[1]);
                return true;
            }
        }

        if (plugin.sd == null || plugin.sd.getIdentityStore() == null)
        {
            msg("SSH daemon is not running.");
            return true;
        }

        SshIdentity identity = plugin.sd.getIdentityStore().get(identifier);
        if (identity == null)
        {
            msg("No SSH identity found for: " + identifier);
            return true;
        }

        String secret = TotpUtil.generateSecret();
        String label = identity.username() != null ? identity.username() : identifier;
        String uri = TotpUtil.buildUri("TotalFreedomMod", label, secret);
        String token = UUID.randomUUID().toString().replace("-", "");

        plugin.sd.getIdentityStore().setTotpSecret(identifier, secret);

        msg("TOTP secret generated for identity: " + identifier);
        msg("User label: " + label);
        msg("Secret (keep private): " + secret);
        msg("Setup URI: " + uri);
        msg("QR page (5 min, one-time): http://<server-ip>:" + port + "/qr?t=" + token);

        SshQrServer.serve(uri, label, port, token);
        return true;
    }
}
