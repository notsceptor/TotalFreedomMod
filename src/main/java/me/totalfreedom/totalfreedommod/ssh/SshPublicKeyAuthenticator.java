package me.totalfreedom.totalfreedommod.ssh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * Authenticates SSH users via public key.
 * Keys are stored in the ssh_auth_keys/ directory under the plugin data folder.
 * Each file is named after the username (no extension) and contains one or more
 * OpenSSH-format authorized_keys entries (ssh-rsa, ssh-ed25519, ecdsa-sha2-*,
 * etc.).
 */
public class SshPublicKeyAuthenticator implements PublickeyAuthenticator {

    private final File authorizedKeysDir;

    public SshPublicKeyAuthenticator(File authorizedKeysDir) {
        this.authorizedKeysDir = authorizedKeysDir;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        File keyFile = new File(authorizedKeysDir, username);

        if (!keyFile.exists()) {
            FLog.warning("SSH public key auth failed for '" + username
                    + "': no authorized_keys file found. Create plugins/TotalFreedomMod/ssh_auth_keys/"
                    + username + " with the user's public key.");
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(keyFile.toPath());
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                try {
                    AuthorizedKeyEntry entry = AuthorizedKeyEntry.parseAuthorizedKeyEntry(trimmed);
                    PublicKey authorizedKey = entry.resolvePublicKey(null, PublicKeyEntryResolver.FAILING);

                    if (key.equals(authorizedKey)) {
                        session.setAttribute(SshDaemon.AUTH_METHOD_KEY, SshAuthMethod.PUBLIC_KEY);
                        FLog.info("SSH public key login successful for user: " + username);
                        return true;
                    }
                } catch (GeneralSecurityException e) {
                    FLog.warning("SSH failed to parse key entry in " + keyFile.getName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            FLog.severe("SSH error reading authorized_keys file for '" + username + "': " + e.getMessage());
        }

        FLog.info("SSH public key auth failed for '" + username + "': no matching key found.");
        return false;
    }
}
