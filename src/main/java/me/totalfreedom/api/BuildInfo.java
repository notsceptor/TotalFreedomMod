package me.totalfreedom.api;

/**
 * Immutable snapshot of the build metadata baked into the plugin jar at build time.
 */
public record BuildInfo(String author, String codename, String version, String date, String buildName)
{
    public String formattedVersion()
    {
        return String.format("%s-%s (%s)", version, codename, buildName);
    }
}
