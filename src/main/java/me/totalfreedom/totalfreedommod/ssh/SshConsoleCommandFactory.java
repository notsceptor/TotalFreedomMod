package me.totalfreedom.totalfreedommod.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.bukkit.Bukkit;

/**
 * Handles one-shot SSH command execution (e.g. {@code ssh user@host 'list'}).
 */
public class SshConsoleCommandFactory implements CommandFactory
{

    private final TotalFreedomMod plugin;

    public SshConsoleCommandFactory(TotalFreedomMod plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public Command createCommand(ChannelSession channel, String command)
    {
        return new SshConsoleCommand(plugin, command);
    }

    public static class SshConsoleCommand implements Command
    {

        private final TotalFreedomMod plugin;
        private final String command;

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;

        public SshConsoleCommand(TotalFreedomMod plugin, String command)
        {
            this.plugin = plugin;
            this.command = command;
        }

        @Override
        public void setInputStream(InputStream in)
        {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out)
        {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err)
        {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback)
        {
            this.callback = callback;
        }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException
        {
            String username = env.getEnv().get(Environment.ENV_USER);

            final SshSession session;
            if (ConfigEntry.SSH_SHOW_USER.getBoolean())
            {
                SshAuthMethod method = channel.getSession().getAttribute(SshDaemon.AUTH_METHOD_KEY);
                session = SshSession.create(
                        username,
                        ConfigEntry.SSH_USER_PREFIX.getString(),
                        method);
            }
            else
            {
                session = null;
            }

            try
            {
                Bukkit.getScheduler().runTask(plugin, () ->
                {
                    FLog.info("[SSH: " + username + "] " + command);
                    SshDispatchContext.dispatch(session, command);
                });
            }
            catch (Exception e)
            {
                FLog.severe("Error processing SSH command from " + username + ": " + e.getMessage());
            }
            finally
            {
                callback.onExit(0);
            }
        }

        @Override
        public void destroy(ChannelSession channel)
        {
        }
    }
}
