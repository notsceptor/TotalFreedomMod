package me.totalfreedom.totalfreedommod.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Log4j2 appender that hands each formatted log line off to a {@link LogLineConsumer}.
 */
public class CallbackLogAppender extends AbstractAppender
{

    @FunctionalInterface
    public interface LogLineConsumer
    {
        void accept(String line, Level level);
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final LogLineConsumer consumer;

    public CallbackLogAppender(String name, LogLineConsumer consumer)
    {
        super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        this.consumer = consumer;
    }

    @Override
    public void append(LogEvent event)
    {
        if (consumer == null)
        {
            return;
        }

        try
        {
            String timestamp;
            synchronized (DATE_FORMAT)
            {
                timestamp = DATE_FORMAT.format(new Date(event.getTimeMillis()));
            }
            String level = event.getLevel().name();
            String message = event.getMessage().getFormattedMessage();

            // Strip Minecraft color codes (§ followed by a character).
            message = message.replaceAll("§[0-9a-fk-or]", "");

            String logLine = "[" + timestamp + " " + level + "]: " + message;
            consumer.accept(logLine, event.getLevel());
        }
        catch (Exception ignored)
        {
        }
    }
}
