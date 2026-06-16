package me.totalfreedom.totalfreedommod.discord;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Paper plugin loader that downloads JDA (and its transitive dependencies)
 * at runtime instead of shading them into the TFM jar.
 */
public class DiscordPluginLoader implements PluginLoader
{

    private static final String JDA_COORDINATES = "net.dv8tion:JDA:5.6.1";

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder)
    {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addDependency(new Dependency(new DefaultArtifact(JDA_COORDINATES), null));
        resolver.addRepository(new RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());
        classpathBuilder.addLibrary(resolver);
    }
}
