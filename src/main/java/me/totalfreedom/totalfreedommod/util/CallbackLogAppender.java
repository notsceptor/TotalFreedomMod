package me.totalfreedom.totalfreedommod.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;
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
    private volatile String[] excludedLoggerPrefixes = {};

    public CallbackLogAppender(String name, LogLineConsumer consumer)
    {
        super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        this.consumer = consumer;
    }

    /**
     * Drop events from loggers whose name starts with any of {@code prefixes}.
     * <p>
     * Needed when the consumer's own delivery path logs: a relay that ships log lines to a remote
     * service and whose client library logs its failures into the same root logger will keep
     * feeding itself, and each failure enqueues the evidence of the previous one.
     *
     * @return this appender, so the exclusions can be set where it is constructed
     */
    public CallbackLogAppender excludeLoggers(String... prefixes)
    {
        this.excludedLoggerPrefixes = prefixes == null ? new String[0] : prefixes.clone();
        return this;
    }

    @Override
    public void append(LogEvent event)
    {
        if (consumer == null || isExcluded(event.getLoggerName()))
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

    private boolean isExcluded(String loggerName)
    {
        if (loggerName == null)
        {
            return false;
        }

        return Stream.of(excludedLoggerPrefixes)
                .anyMatch(loggerName::startsWith);
    }
}
