package me.totalfreedom.totalfreedommod.ssh;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jline.terminal.Terminal;

/**
 * Log4j2 appender that streams server log output to an SSH session's terminal.
 */
public class SshLogAppender extends AbstractAppender
{

    private final Terminal terminal;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public SshLogAppender(String name, Terminal terminal)
    {
        super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        this.terminal = terminal;
    }

    @Override
    public void append(LogEvent event)
    {
        if (terminal == null)
        {
            return;
        }

        try
        {
            String timestamp = dateFormat.format(new Date(event.getTimeMillis()));
            String level = event.getLevel().name();
            String message = event.getMessage().getFormattedMessage();

            // Strip Minecraft color codes (§ followed by a character)
            message = message.replaceAll("§[0-9a-fk-or]", "");

            String logLine = "[" + timestamp + " " + level + "]: " + message;

            // Write the log line to the terminal
            terminal.writer().println(logLine);
            terminal.writer().flush();
        }
        catch (Exception e)
        {
        }
    }
}
