package me.totalfreedom.totalfreedommod.wordle;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import com.destroystokyo.paper.profile.PlayerProfile;

import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
import me.totalfreedom.totalfreedommod.wordle.WordleGame.LetterResult;

final class HeadCache
{
    private final Map<LetterResult, Map<Character, ItemStack>> heads = new EnumMap<>(LetterResult.class);

    HeadCache(final WordList wordList)
    {
        for (LetterResult result : LetterResult.values())
        {
            final Map<Character, ItemStack> letterHeads = new HashMap<>();

            for (char letter = 'A'; letter <= 'Z'; letter++)
            {
                final String hash = wordList.headTexture(result, letter);
                letterHeads.put(letter, createHead(hash, letter, result));
            }

            heads.put(result, Map.copyOf(letterHeads));
        }
    }

    ItemStack get(final LetterResult result, final char letter)
    {
        final ItemStack head = heads.get(result).get(letter);
        if (head == null)
            throw new IllegalArgumentException("No Wordle head for letter: " + letter);

        return head.clone();
    }

    private static ItemStack createHead(final String hash, final char letter, final LetterResult result)
    {
        final ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta)item.getItemMeta();
        final PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(hash.getBytes(StandardCharsets.UTF_8)));
        final PlayerTextures textures = profile.getTextures();

        try
        {
            textures.setSkin(URI.create("http://textures.minecraft.net/texture/" + hash).toURL());
        }
        catch (MalformedURLException exception)
        {
            throw new IllegalStateException("Invalid Wordle head texture URL.", exception);
        }

        profile.setTextures(textures);
        meta.setPlayerProfile(profile);
        meta.displayName(MessageUtils.parse(color(result) + "<letter>",
                MessageUtils.unparsed("letter", String.valueOf(letter))));
        item.setItemMeta(meta);
        return item;
    }

    private static String color(final LetterResult result)
    {
        return switch (result)
        {
            case GREEN -> "<green>";
            case ORANGE -> "<color:#FFA500>";
            case GRAY -> "<gray>";
        };
    }
}
