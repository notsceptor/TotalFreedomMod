package me.totalfreedom.totalfreedommod.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import org.bukkit.Bukkit;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Creates interactive console shell sessions for SSH clients.
 * Each session gets its own JLine LineReader with tab completion and log streaming.
 */
public class SshConsoleShellFactory implements ShellFactory
{

    private final TotalFreedomMod plugin;

    public SshConsoleShellFactory(TotalFreedomMod plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public Command createShell(ChannelSession channel)
    {
        return new SshConsoleShell(plugin);
    }

    /**
     * An interactive console shell for a single SSH session.
     */
    public static class SshConsoleShell implements Command, Runnable
    {

        private final TotalFreedomMod plugin;

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Environment environment;
        private Thread thread;

        private Terminal terminal;
        private LineReader lineReader;
        private SshLogAppender logAppender;
        // Captured at start() so dispatch in run() doesn't need ChannelSession.
        private SshSession sshSession;

        public SshConsoleShell(TotalFreedomMod plugin)
        {
            this.plugin = plugin;
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
            this.environment = env;

            try
            {
                // Bypass JLine 3's TerminalBuilder and SPI provider resolution entirely.
                String termType = env.getEnv().getOrDefault(Environment.ENV_TERM, "xterm-256color");
                terminal = new org.jline.terminal.impl.ExternalTerminal(
                        "TFM-SSH",
                        termType,
                        in,
                        out,
                        java.nio.charset.StandardCharsets.UTF_8
                );

                // Build a LineReader with tab completion
                lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(new SshCommandCompleter(plugin))
                        .build();

                // Attach a log appender so server logs stream to this session
                logAppender = new SshLogAppender("SshLogAppender-" + env.getEnv().get(Environment.ENV_USER), terminal);
                logAppender.start();
                ((Logger) LogManager.getRootLogger()).addAppender(logAppender);

                String username = env.getEnv().get(Environment.ENV_USER);
                SshAuthMethod method = channel.getSession().getAttribute(SshDaemon.AUTH_METHOD_KEY);
                if (ConfigEntry.SSH_SHOW_USER.getBoolean())
                {
                    sshSession = SshSession.create(
                            username,
                            ConfigEntry.SSH_USER_PREFIX.getString(),
                            method);
                }
                else
                {
                    sshSession = null;
                }

                // Start the reader thread
                thread = new Thread(this, "SSHD ConsoleShell " + username);
                thread.setDaemon(true);
                thread.start();
            }
            catch (Exception e)
            {
                throw new IOException("Error starting SSH shell", e);
            }
        }

        @Override
        public void destroy(ChannelSession channel)
        {
            // Remove log appender
            if (logAppender != null)
            {
                ((Logger) LogManager.getRootLogger()).removeAppender(logAppender);
                logAppender.stop();
            }

            // Close terminal
            if (terminal != null)
            {
                try
                {
                    terminal.close();
                }
                catch (IOException e)
                {
                    // Ignore
                }
            }

            // Interrupt reader thread
            if (thread != null && thread.isAlive())
            {
                thread.interrupt();
            }
        }

        @Override
        public void run()
        {
            String username = environment.getEnv().get(Environment.ENV_USER);

            try
            {
                printPreamble();

                while (true)
                {
                    String command;
                    try
                    {
                        command = lineReader.readLine("> ");
                    }
                    catch (UserInterruptException e)
                    {
                        continue;
                    }
                    catch (EndOfFileException e)
                    {
                        break;
                    }

                    if (command == null)
                    {
                        continue;
                    }

                    command = command.trim();
                    if (command.isEmpty())
                    {
                        continue;
                    }

                    if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit"))
                    {
                        break;
                    }

                    // Dispatch command on the main server thread
                    final String cmd = command;
                    Bukkit.getScheduler().runTask(plugin, () ->
                    {
                        FLog.info("[SSH: " + username + "] " + cmd);
                        SshDispatchContext.dispatch(sshSession, cmd);
                    });
                }
            }
            catch (Exception e)
            {
                FLog.severe("Error processing SSH shell for user: " + username);
                FLog.severe(e);
            }
            finally
            {
                callback.onExit(0);
            }
        }

        private void printPreamble() throws IOException
        {
            terminal.writer().println("TotalFreedomMod version " + plugin.getDescription().getVersion());
            terminal.writer().println("Connected to: " + Bukkit.getServer().getName());
            terminal.writer().println("- " + Bukkit.getServer().getMotd());
            terminal.writer().println();
            terminal.writer().println("Type 'exit' to disconnect.");
            terminal.writer().println("===============================================");
            terminal.writer().flush();
        }
    }
}
