package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
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
        usage = "/<command> <identity>")
public class Command_sshtotp extends FreedomCommand
{
    /** 
     * It's important to note that identity is the username identity of the player as their file name is set. 
     */
    @CommandDispatchTarget(pattern = "<identity>")
    public boolean sshtotp(CommandContext ctx, String identity)
    {
        if (plugin.sd == null || plugin.sd.getIdentityStore() == null)
        {
            msg("SSH daemon is not running.");
            return true;
        }

        SshIdentity sshIdentity = plugin.sd.getIdentityStore().get(identity);
        if (sshIdentity == null)
        {
            msg("No SSH identity found for: " + identity);
            return true;
        }

        String secret = TotpUtil.generateSecret();
        String uri = TotpUtil.buildUri("TotalFreedomMod", identity, secret);
        String token = UUID.randomUUID().toString().replace("-", "");

        plugin.sd.getIdentityStore().setTotpSecret(identity, secret);

        msg("TOTP secret generated for identity: " + identity);
        msg("Secret (keep private): " + secret);
        msg("Setup URI: " + uri);
        msg("QR page (5 min, one-time): http://" + ConfigEntry.SERVER_ADDRESS.getString() + ":" + ConfigEntry.SSH_QR_PORT.getInteger() + "/qr?t=" + token);

        SshQrServer.enqueue(uri, identity, token);
        return true;
    }

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
