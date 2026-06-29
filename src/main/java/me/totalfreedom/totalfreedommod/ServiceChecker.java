package me.totalfreedom.totalfreedommod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.scheduler.BukkitRunnable;

public class ServiceChecker extends FreedomService
{

    private String lastStatus = null;
    private boolean isChecking = false;

    public ServiceChecker(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
    }

    @Override
    protected void onStop()
    {
    }

    public void checkServicesAsync(final ServiceCheckCallback callback)
    {
        if (isChecking)
        {
            callback.onResult("Already checking services...", false);
            return;
        }

        isChecking = true;

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String serviceUrl = ConfigEntry.SERVICE_CHECKER_URL.getString();
                    if (serviceUrl == null || serviceUrl.isEmpty())
                    {
                        // Default to mcping.me API (more reliable than status.mojang.com)
                        serviceUrl = "https://mcping.me/api/services";
                    }

                    final URL url = URI.create(serviceUrl).toURL();
                    final URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "TotalFreedomMod/" + TotalFreedomMod.pluginVersion);

                    String statusRaw;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
                    {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            response.append(line);
                        }
                        statusRaw = response.toString();
                    }

                    // Try to parse and format JSON response for better readability
                    final String status;
                    if (statusRaw.startsWith("{") && (statusRaw.contains("Operational") || statusRaw.contains("PossibleProblems") || statusRaw.contains("DefiniteProblems")))
                    {
                        // Format JSON response for better readability
                        status = formatMojangStatusResponse(statusRaw);
                    }
                    else
                    {
                        status = statusRaw;
                    }

                    lastStatus = status;

                    if (!plugin.isEnabled())
                    {
                        return;
                    }

                    new BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            callback.onResult(status, true);
                            isChecking = false;
                        }
                    }.runTask(plugin);

                }
                catch (java.net.UnknownHostException ex)
                {
                    FLog.warning("Could not resolve hostname for service checker. Check your DNS settings or network connectivity.");
                    if (plugin.isEnabled())
                    {
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                callback.onResult("Could not connect to service status API. Check your DNS settings or network connectivity.", false);
                                isChecking = false;
                            }
                        }.runTask(plugin);
                    }
                }
                catch (java.net.SocketTimeoutException ex)
                {
                    FLog.warning("Service checker request timed out.");
                    if (plugin.isEnabled())
                    {
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                callback.onResult("Service status request timed out. The API may be slow or unavailable.", false);
                                isChecking = false;
                            }
                        }.runTask(plugin);
                    }
                }
                catch (Exception ex)
                {
                    FLog.severe(ex);
                    if (plugin.isEnabled())
                    {
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                String errorMsg = ex.getMessage();
                                if (errorMsg == null || errorMsg.isEmpty())
                                {
                                    errorMsg = ex.getClass().getSimpleName();
                                }
                                callback.onResult("Error checking services: " + errorMsg, false);
                                isChecking = false;
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public String getLastStatus()
    {
        return lastStatus;
    }

    private String formatMojangStatusResponse(String json)
    {
        // Simple JSON formatting for mcping.me API response
        // Example: {"minecraft.net":"Operational","session.minecraft.net":"Operational",...}
        try
        {
            StringBuilder formatted = new StringBuilder();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}"))
            {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs)
                {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2)
                    {
                        String key = keyValue[0].trim().replace("\"", "");
                        String value = keyValue[1].trim().replace("\"", "");
                        formatted.append(key).append(": ").append(value).append("\n");
                    }
                }
            }
            else
            {
                formatted.append(json);
            }
            return formatted.toString().trim();
        }
        catch (Exception e)
        {
            // If formatting fails, return original JSON
            return json;
        }
    }

    public interface ServiceCheckCallback
    {
        void onResult(String status, boolean success);
    }
}

