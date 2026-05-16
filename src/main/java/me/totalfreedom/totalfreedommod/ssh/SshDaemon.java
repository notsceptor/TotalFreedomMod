package me.totalfreedom.totalfreedommod.ssh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

/**
 * SSH daemon service for TotalFreedomMod.
 */
public class SshDaemon extends FreedomService {

    private SshServer sshd;
    private int port;

    public SshDaemon(TotalFreedomMod plugin) {
        super(plugin);
    }

    @Override
    protected void onStart() {
        if (!ConfigEntry.SSH_ENABLED.getBoolean()) {
            return;
        }

        port = ConfigEntry.SSH_PORT.getInteger();
        String authMode = ConfigEntry.SSH_AUTH_MODE.getString().toLowerCase();

        File dataFolder = plugin.getDataFolder();
        File authorizedKeysDir = new File(dataFolder, "auth_keys");
        if (!authorizedKeysDir.exists()) {
            authorizedKeysDir.mkdirs();
        }

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);

        // Host key provider — auto-generates on first run
        Path hostKeyPath = new File(dataFolder, "ssh_host_key").toPath();
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));

        // Authentication
        boolean usePassword = authMode.equals("password") || authMode.equals("both");
        boolean usePublicKey = authMode.equals("key") || authMode.equals("both");

        if (usePassword) {
            sshd.setPasswordAuthenticator(new SshPasswordAuthenticator());
        }

        if (usePublicKey) {
            sshd.setPublickeyAuthenticator(new SshPublicKeyAuthenticator(authorizedKeysDir));
        }

        if (!usePassword && !usePublicKey) {
            FLog.warning("SSH auth_mode is invalid ('" + authMode + "'). Defaulting to password.");
            sshd.setPasswordAuthenticator(new SshPasswordAuthenticator());
        }

        // Shell and command factories
        sshd.setShellFactory(new SshConsoleShellFactory(plugin));
        sshd.setCommandFactory(new SshConsoleCommandFactory(plugin));

        try {
            sshd.start();
            FLog.info("SSH daemon started. Listening on port: " + port);
        } catch (IOException e) {
            FLog.severe("Failed to start SSH daemon on port " + port + "!");
            FLog.severe(e);
        }
    }

    @Override
    protected void onStop() {
        if (sshd != null) {
            try {
                sshd.stop(true);
                FLog.info("SSH daemon stopped.");
            } catch (Exception e) {
                FLog.severe("Error stopping SSH daemon.");
                FLog.severe(e);
            }
        }
    }
}
