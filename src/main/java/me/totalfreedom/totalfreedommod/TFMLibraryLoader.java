package me.totalfreedom.totalfreedommod;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves TFM's heavyweight runtime libraries from Maven Central at plugin
 * load time instead of shading them into the jar.
 */
public class TFMLibraryLoader implements PluginLoader
{

    private static final String[] LIBRARIES = {
            "net.dv8tion:JDA:5.6.1",
            "org.apache.sshd:sshd-core:2.17.1"
    };

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder)
    {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        for (String coordinates : LIBRARIES)
        {
            resolver.addDependency(new Dependency(new DefaultArtifact(coordinates), null));
        }
        resolver.addRepository(new RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());
        classpathBuilder.addLibrary(resolver);
    }
}
