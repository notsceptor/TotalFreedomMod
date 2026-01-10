package me.totalfreedom.totalfreedommod.util;

import java.io.File;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class FUtil
{

    private static final Random RANDOM = new Random();
    // See https://github.com/TotalFreedom/License - None of the listed names may be removed.
    public static final List<String> DEVELOPERS = Arrays.asList("Madgeek1450", "Prozza", "Wild1145", "WickedGamingUK", "aggelosQQ", "aokod", "rptt", "ERR_666");
    public static String DATE_STORAGE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final Map<String, NamedTextColor> CHAT_COLOR_NAMES = new HashMap<>();
    public static final List<NamedTextColor> CHAT_COLOR_POOL = Arrays.asList(
            NamedTextColor.DARK_BLUE,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_RED,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.GOLD,
            NamedTextColor.BLUE,
            NamedTextColor.GREEN,
            NamedTextColor.AQUA,
            NamedTextColor.RED,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.YELLOW);
    
    // Backward compatibility - deprecated
    @Deprecated
    public static final Map<String, ChatColor> CHAT_COLOR_NAMES_LEGACY = new HashMap<>();
    @Deprecated
    public static final List<ChatColor> CHAT_COLOR_POOL_LEGACY = Arrays.asList(
            ChatColor.DARK_BLUE,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_RED,
            ChatColor.DARK_PURPLE,
            ChatColor.GOLD,
            ChatColor.BLUE,
            ChatColor.GREEN,
            ChatColor.AQUA,
            ChatColor.RED,
            ChatColor.LIGHT_PURPLE,
            ChatColor.YELLOW);

    static
    {
        for (NamedTextColor color : CHAT_COLOR_POOL)
        {
            CHAT_COLOR_NAMES.put(color.toString().toLowerCase().replace("_", ""), color);
        }
        for (ChatColor chatColor : CHAT_COLOR_POOL_LEGACY)
        {
            CHAT_COLOR_NAMES_LEGACY.put(chatColor.name().toLowerCase().replace("_", ""), chatColor);
        }
    }

    private FUtil()
    {
    }

    public static void cancel(BukkitTask task)
    {
        if (task == null)
        {
            return;
        }

        try
        {
            task.cancel();
        }
        catch (Exception ex)
        {
        }
    }

    public static void bcastMsg(Component component)
    {
        // Serialize to ANSI for console, Component for players
        String ansiMessage = ANSIComponentSerializer.ansi().serialize(component);
        Bukkit.getConsoleSender().sendMessage(ansiMessage);

        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.sendMessage(component);
        }
    }

    public static void bcastMsg(String message, NamedTextColor color)
    {
        Component component = Component.text(message);
        if (color != null)
        {
            component = component.color(color);
        }
        bcastMsg(component);
    }

    @Deprecated
    public static void bcastMsg(String message, ChatColor color)
    {
        NamedTextColor namedColor = color != null ? AdventureUtil.chatColorToNamedTextColor(color) : null;
        bcastMsg(message, namedColor);
    }

    public static void bcastMsg(String message)
    {
        bcastMsg(Component.text(message));
    }

    public static void playerMsg(CommandSender sender, Component component)
    {
        if (sender == null || component == null)
        {
            return;
        }
        sender.sendMessage(component);
    }

    public static void playerMsg(CommandSender sender, String message, NamedTextColor color)
    {
        Component component = Component.text(message);
        if (color != null)
        {
            component = component.color(color);
        }
        playerMsg(sender, component);
    }

    @Deprecated
    public static void playerMsg(CommandSender sender, String message, ChatColor color)
    {
        NamedTextColor namedColor = color != null ? AdventureUtil.chatColorToNamedTextColor(color) : null;
        playerMsg(sender, message, namedColor);
    }

    public static void playerMsg(CommandSender sender, String message)
    {
        playerMsg(sender, message, NamedTextColor.GRAY);
    }

    public static void setFlying(Player player, boolean flying)
    {
        player.setAllowFlight(true);
        player.setFlying(flying);
    }

    public static void adminAction(String adminName, String action, boolean isRed)
    {
        FUtil.bcastMsg(adminName + " - " + action, (isRed ? NamedTextColor.RED : NamedTextColor.AQUA));
    }

    public static String formatLocation(Location location)
    {
        return String.format("%s: (%d, %d, %d)",
                location.getWorld().getName(),
                Math.round(location.getX()),
                Math.round(location.getY()),
                Math.round(location.getZ()));
    }

    public static boolean deleteFolder(final File file)
    {
        if (file.exists() && file.isDirectory())
        {
            return FileUtils.deleteQuietly(file);
        }
        return false;
    }

    public static void deleteCoreDumps()
    {
        final File[] coreDumps = new File(".").listFiles(file -> file.getName().startsWith("java.core"));

        for (File dump : coreDumps)
        {
            FLog.info("Removing core dump file: " + dump.getName());
            dump.delete();
        }
    }

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?"
            + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?"
            + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?"
            + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?"
            + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?"
            + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?"
            + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);

    public static Date parseDateOffset(String time)
    {
        Matcher m = TIME_PATTERN.matcher(time);
        int years = 0;
        int months = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        boolean found = false;
        while (m.find())
        {
            if (m.group() == null || m.group().isEmpty())
            {
                continue;
            }
            for (int i = 0; i < m.groupCount(); i++)
            {
                if (m.group(i) != null && !m.group(i).isEmpty())
                {
                    found = true;
                    break;
                }
            }
            if (found)
            {
                years = parseGroup(m, 1);
                months = parseGroup(m, 2);
                weeks = parseGroup(m, 3);
                days = parseGroup(m, 4);
                hours = parseGroup(m, 5);
                minutes = parseGroup(m, 6);
                seconds = parseGroup(m, 7);
                break;
            }
        }
        if (!found)
        {
            return null;
        }

        Calendar c = new GregorianCalendar();

        if (years > 0)
        {
            c.add(Calendar.YEAR, years);
        }
        if (months > 0)
        {
            c.add(Calendar.MONTH, months);
        }
        if (weeks > 0)
        {
            c.add(Calendar.WEEK_OF_YEAR, weeks);
        }
        if (days > 0)
        {
            c.add(Calendar.DAY_OF_MONTH, days);
        }
        if (hours > 0)
        {
            c.add(Calendar.HOUR_OF_DAY, hours);
        }
        if (minutes > 0)
        {
            c.add(Calendar.MINUTE, minutes);
        }
        if (seconds > 0)
        {
            c.add(Calendar.SECOND, seconds);
        }

        return c.getTime();
    }

    private static int parseGroup(Matcher m, int group)
    {
        String value = m.group(group);
        if (value != null && !value.isEmpty())
        {
            return Integer.parseInt(value);
        }
        return 0;
    }

    public static String playerListToNames(Set<OfflinePlayer> players)
    {
        List<String> names = new ArrayList<>();
        for (OfflinePlayer player : players)
        {
            names.add(player.getName());
        }
        return StringUtils.join(names, ", ");
    }

    public static String dateToString(Date date)
    {
        return new SimpleDateFormat(DATE_STORAGE_FORMAT, Locale.ENGLISH).format(date);
    }

    public static Date stringToDate(String dateString)
    {
        try
        {
            return new SimpleDateFormat(DATE_STORAGE_FORMAT, Locale.ENGLISH).parse(dateString);
        }
        catch (ParseException pex)
        {
            return new Date(0L);
        }
    }

    public static boolean isFromHostConsole(String senderName)
    {
        return ConfigEntry.HOST_SENDER_NAMES.getList().contains(senderName.toLowerCase());
    }

    public static boolean fuzzyIpMatch(String a, String b, int octets)
    {
        boolean match = true;

        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");

        if (aParts.length != 4 || bParts.length != 4)
        {
            return false;
        }

        if (octets > 4)
        {
            octets = 4;
        }
        else if (octets < 1)
        {
            octets = 1;
        }

        for (int i = 0; i < octets && i < 4; i++)
        {
            if (aParts[i].equals("*") || bParts[i].equals("*"))
            {
                continue;
            }

            if (!aParts[i].equals(bParts[i]))
            {
                match = false;
                break;
            }
        }

        return match;
    }

    public static String getFuzzyIp(String ip)
    {
        final String[] ipParts = ip.split("\\.");
        if (ipParts.length == 4)
        {
            return String.format("%s.%s.*.*", ipParts[0], ipParts[1]);
        }

        return ip;
    }

    //getField: Borrowed from WorldEdit
    @SuppressWarnings("unchecked")
    public static <T> T getField(Object from, String name)
    {
        Class<?> checkClass = from.getClass();
        do
        {
            try
            {
                Field field = checkClass.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(from);

            }
            catch (NoSuchFieldException | IllegalAccessException ex)
            {
            }
        } while (checkClass.getSuperclass() != Object.class
                && ((checkClass = checkClass.getSuperclass()) != null));

        return null;
    }

    public static NamedTextColor randomChatColor()
    {
        return CHAT_COLOR_POOL.get(RANDOM.nextInt(CHAT_COLOR_POOL.size()));
    }

    @Deprecated
    public static ChatColor randomChatColorLegacy()
    {
        return CHAT_COLOR_POOL_LEGACY.get(RANDOM.nextInt(CHAT_COLOR_POOL_LEGACY.size()));
    }

    public static Component colorize(String string)
    {
        return AdventureUtil.translateAlternateColorCodes('&', string);
    }

    @Deprecated
    public static String colorizeLegacy(String string)
    {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static Date getUnixDate(long unix)
    {
        return new Date(unix * 1000);
    }

    public static long getUnixTime()
    {
        return System.currentTimeMillis() / 1000L;
    }

    public static long getUnixTime(Date date)
    {
        if (date == null)
        {
            return 0;
        }

        return date.getTime() / 1000L;
    }

    public static String getNmsVersion()
    {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

}
