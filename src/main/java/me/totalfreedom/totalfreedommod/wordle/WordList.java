package me.totalfreedom.totalfreedommod.wordle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import me.totalfreedom.totalfreedommod.wordle.WordleGame.LetterResult;

final class WordList
{
    private static final String CONFIG_RESOURCE = "fun/wordle.json";

    private final JsonObject configuration;
    private final List<String> answers;
    private final List<WordleHelpPage> helpPages;
    private final Map<String, MenuItem> interactables;
    private final JsonObject messages;
    private final int baseWinPayout;
    private final int unusedGuessPayout;

    record MenuItem(Material material, String name, List<String> lore)
    {
        MenuItem
        {
            lore = List.copyOf(lore);
        }
    }

    WordList(final FreedomAPI plugin)
    {
        configuration = loadConfiguration(plugin);
        final JsonElement answerElement = configuration.get("answers");
        if (answerElement == null || !answerElement.isJsonArray())
            throw new IllegalStateException("Wordle configuration must contain an answers array.");

        answers = answerElement.getAsJsonArray().asList().stream()
                .map(JsonElement::getAsString)
                .map(word -> word.strip().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();

        if (answers.isEmpty())
            throw new IllegalStateException("Wordle configuration has no answer words.");

        if (answers.stream().anyMatch(word -> !WordleGame.isValidWord(word)))
            throw new IllegalStateException("Wordle answer words must be exactly five letters A-Z.");

        helpPages = loadHelpPages(configuration);
        interactables = loadInteractables(configuration);
        messages = loadMessages(configuration);
        final JsonObject payouts = configuration.getAsJsonObject("payouts");
        baseWinPayout = nonNegativeInteger(payouts, "base_win", "payouts");
        unusedGuessPayout = nonNegativeInteger(payouts, "unused_guess_bonus", "payouts");
        if ((long)baseWinPayout + (WordleGame.MAX_GUESSES - 1L) * unusedGuessPayout > Integer.MAX_VALUE)
            throw new IllegalStateException("Maximum Wordle payout exceeds the supported ECO amount.");

        try
        {
            Files.deleteIfExists(plugin.getDataFolder().toPath().resolve("wordle.yml"));
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not remove the migrated Wordle YAML configuration.", exception);
        }
    }

    String headTexture(final LetterResult result, final char letter)
    {
        final JsonObject heads = configuration.getAsJsonObject("heads");
        final JsonObject group = heads == null ? null : heads.getAsJsonObject(result.name().toLowerCase(Locale.ROOT));
        final JsonElement texture = group == null ? null : group.get(String.valueOf(letter));
        if (texture == null || !texture.isJsonPrimitive() || !texture.getAsJsonPrimitive().isString()
                || !texture.getAsString().matches("[a-f0-9]{20,64}"))
            throw new IllegalStateException("Missing or invalid Wordle texture for " + result + " " + letter);

        return texture.getAsString();
    }

    String select(final long seed)
    {
        return answers.get(new Random(seed).nextInt(answers.size()));
    }

    List<WordleHelpPage> helpPages()
    {
        return helpPages;
    }

    MenuItem interactable(final String name)
    {
        final MenuItem item = interactables.get(name);
        if (item == null)
            throw new IllegalArgumentException("Unknown Wordle interactable: " + name);

        return item;
    }

    String message(final String name)
    {
        return messages.get(name).getAsString();
    }

    List<String> messageLines(final String name)
    {
        return messages.getAsJsonArray(name).asList()
                .stream()
                .map(JsonElement::getAsString)
                .toList();
    }

    int baseWinPayout()
    {
        return baseWinPayout;
    }

    int unusedGuessPayout()
    {
        return unusedGuessPayout;
    }

    private static List<WordleHelpPage> loadHelpPages(final JsonObject config)
    {
        final JsonElement configuredPages = config.get("how_to_play");
        if (configuredPages == null || !configuredPages.isJsonArray() || configuredPages.getAsJsonArray().isEmpty())
            throw new IllegalStateException("Wordle configuration must contain at least one how_to_play page.");

        return configuredPages.getAsJsonArray().asList().stream()
                .map(element ->
                {
                    if (!element.isJsonObject())
                        throw new IllegalStateException("Every how_to_play page must be a JSON object.");

                    final JsonObject page = element.getAsJsonObject();
                    final String heading = requiredString(page, "heading", "how_to_play page");
                    final JsonElement paragraphsElement = page.get("paragraphs");
                    if (paragraphsElement == null || !paragraphsElement.isJsonArray()
                            || paragraphsElement.getAsJsonArray().isEmpty())
                        throw new IllegalStateException("Every how_to_play page must contain paragraphs.");

                    final List<String> paragraphs = paragraphsElement.getAsJsonArray().asList().stream()
                            .map(JsonElement::getAsString)
                            .toList();
                    if (paragraphs.stream().anyMatch(String::isBlank))
                        throw new IllegalStateException("Wordle how_to_play paragraphs cannot be blank.");

                    return new WordleHelpPage(heading, paragraphs);
                })
                .toList();
    }

    private static Map<String, MenuItem> loadInteractables(final JsonObject config)
    {
        final JsonObject configuredItems = config.getAsJsonObject("interactables");
        if (configuredItems == null)
            throw new IllegalStateException("Wordle configuration must contain an interactables object.");

        return configuredItems.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> loadMenuItem(entry.getKey(), entry.getValue())));
    }

    private static JsonObject loadMessages(final JsonObject config)
    {
        final JsonObject messages = config.getAsJsonObject("messages");
        if (messages == null)
            throw new IllegalStateException("Wordle configuration must contain a messages object.");

        for (String key : List.of("anvil_input_lore", "anvil_result_lore"))
        {
            final JsonElement lines = messages.get(key);
            if (lines == null || !lines.isJsonArray()
                    || lines.getAsJsonArray().asList().stream()
                    .anyMatch(line -> !line.isJsonPrimitive() || !line.getAsJsonPrimitive().isString()))
                throw new IllegalStateException("Wordle messages." + key + " must be an array of strings.");
        }

        for (String key : List.of("board_title", "help_book_title", "leaderboard_book_title",
                "leaderboard_header", "leaderboard_empty", "anvil_title", "anvil_input_name",
                "anvil_result_name", "load_failed", "leaderboard_load_failed", "invalid_anvil_guess",
                "invalid_guess", "win_title", "win_subtitle", "win_chat", "loss_title",
                "loss_subtitle", "loss_chat"))
            requiredString(messages, key, "messages");

        return messages;
    }

    private static MenuItem loadMenuItem(final String name, final JsonElement element)
    {
        if (!element.isJsonObject())
            throw new IllegalStateException("Wordle interactables." + name + " must be an object.");

        final JsonObject item = element.getAsJsonObject();
        final String materialName = requiredString(item, "material", "interactables." + name);
        final Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir())
            throw new IllegalStateException("Wordle interactables." + name + ".material must be a valid item material.");

        final String displayName = requiredString(item, "name", "interactables." + name);
        final JsonElement loreElement = item.get("lore");
        if (loreElement == null || !loreElement.isJsonArray())
            throw new IllegalStateException("Wordle interactables." + name + ".lore must be an array of strings.");

        final JsonArray loreArray = loreElement.getAsJsonArray();
        final List<String> lore = loreArray.asList().stream()
                .map(line ->
                {
                    if (!line.isJsonPrimitive() || !line.getAsJsonPrimitive().isString())
                        throw new IllegalStateException("Wordle interactables." + name + ".lore must contain only strings.");
                    return line.getAsString();
                })
                .toList();

        return new MenuItem(material, displayName, lore);
    }

    private static String requiredString(final JsonObject object, final String key, final String section)
    {
        final JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank())
            throw new IllegalStateException("Wordle " + section + " must contain a non-blank " + key + ".");

        return value.getAsString();
    }

    private static int nonNegativeInteger(final JsonObject object, final String key, final String section)
    {
        final JsonElement value = object == null ? null : object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalStateException("Wordle " + section + "." + key + " must be a non-negative integer.");

        try
        {
            final int amount = value.getAsBigDecimal().intValueExact();
            if (amount < 0)
                throw new IllegalStateException("Wordle " + section + "." + key + " must be non-negative.");
            return amount;
        }
        catch (ArithmeticException exception)
        {
            throw new IllegalStateException("Wordle " + section + "." + key + " must be a non-negative integer.",
                    exception);
        }
    }

    private static JsonObject loadConfiguration(final FreedomAPI plugin)
    {
        final Path pluginDirectory = plugin.getDataFolder().toPath();
        final Path funDirectory = pluginDirectory.resolve("fun");
        final Path configPath = funDirectory.resolve("wordle.json");
        final Path previousConfigPath = pluginDirectory.resolve("eco").resolve("wordle.json");
        final Path legacyPath = pluginDirectory.resolve("wordle.yml");

        try
        {
            Files.createDirectories(funDirectory);
            final boolean migratingPreviousConfig = !Files.exists(configPath) && Files.exists(previousConfigPath);
            final JsonObject config;
            if (Files.exists(configPath))
                config = readJson(configPath);
            else if (migratingPreviousConfig)
                config = readJson(previousConfigPath);
            else if (Files.exists(legacyPath))
                config = readLegacyYaml(legacyPath);
            else
            {
                plugin.saveResource(CONFIG_RESOURCE, false);
                config = readJson(configPath);
            }

            if (mergeDefaults(plugin, config))
                writeConfiguration(configPath, config);
            else if (!Files.exists(configPath))
                writeConfiguration(configPath, config);

            if (migratingPreviousConfig)
                Files.deleteIfExists(previousConfigPath);
            removeEmptyDirectory(previousConfigPath.getParent());

            return config;
        }
        catch (IOException | InvalidConfigurationException exception)
        {
            throw new IllegalStateException("Could not load Wordle configuration from " + configPath, exception);
        }
        catch (IllegalArgumentException exception)
        {
            throw new IllegalStateException("Could not install default Wordle configuration resource: " + CONFIG_RESOURCE,
                    exception);
        }
    }

    private static JsonObject readJson(final Path path) throws IOException
    {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))
        {
            final JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject())
                throw new IllegalStateException("Wordle configuration must be a JSON object: " + path);
            return element.getAsJsonObject();
        }
    }

    private static JsonObject readLegacyYaml(final Path path) throws IOException, InvalidConfigurationException
    {
        final YamlConfiguration legacy = new YamlConfiguration();
        legacy.load(path.toFile());
        final JsonObject config = new JsonObject();
        final JsonArray answers = new JsonArray();
        legacy.getStringList("answers").forEach(answers::add);
        config.add("answers", answers);

        final JsonObject heads = new JsonObject();
        final ConfigurationSection legacyHeads = legacy.getConfigurationSection("heads");
        if (legacyHeads != null)
        {
            for (LetterResult result : LetterResult.values())
            {
                final ConfigurationSection group = legacyHeads.getConfigurationSection(result.name().toLowerCase(Locale.ROOT));
                if (group == null)
                    continue;

                final JsonObject textures = new JsonObject();
                for (char letter = 'A'; letter <= 'Z'; letter++)
                {
                    final String hash = group.getString(String.valueOf(letter));
                    if (hash != null)
                        textures.addProperty(String.valueOf(letter), hash);
                }
                heads.add(result.name().toLowerCase(Locale.ROOT), textures);
            }
        }
        config.add("heads", heads);
        return config;
    }

    private static boolean mergeDefaults(final FreedomAPI plugin, final JsonObject config)
            throws IOException
    {
        final JsonObject defaults;
        try (InputStream stream = plugin.getResource(CONFIG_RESOURCE))
        {
            if (stream == null)
                throw new IllegalStateException("Missing default Wordle configuration resource: " + CONFIG_RESOURCE);

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                final JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject())
                    throw new IllegalStateException("Default Wordle configuration must be a JSON object.");
                defaults = element.getAsJsonObject();
            }
        }

        final JsonObject defaultHeads = defaults.getAsJsonObject("heads");
        if (defaultHeads == null)
            throw new IllegalStateException("Default Wordle configuration is missing head textures.");

        boolean updated = false;
        for (String section : List.of("how_to_play", "payouts", "interactables", "messages"))
        {
            if (!config.has(section))
            {
                config.add(section, defaults.get(section).deepCopy());
                updated = true;
            }
        }

        final JsonObject defaultMessages = defaults.getAsJsonObject("messages");
        final JsonObject messages = config.getAsJsonObject("messages");
        if (defaultMessages != null && messages != null)
        {
            for (String key : defaultMessages.keySet())
            {
                if (!messages.has(key))
                {
                    messages.add(key, defaultMessages.get(key).deepCopy());
                    updated = true;
                }
            }
        }

        final JsonObject defaultInteractables = defaults.getAsJsonObject("interactables");
        final JsonObject interactables = config.getAsJsonObject("interactables");
        if (defaultInteractables != null && interactables != null)
        {
            for (String key : defaultInteractables.keySet())
            {
                if (!interactables.has(key))
                {
                    interactables.add(key, defaultInteractables.get(key).deepCopy());
                    updated = true;
                    continue;
                }

                final JsonObject defaultItem = defaultInteractables.getAsJsonObject(key);
                final JsonObject item = interactables.getAsJsonObject(key);
                if (defaultItem == null || item == null)
                    continue;

                for (String itemKey : defaultItem.keySet())
                {
                    if (!item.has(itemKey))
                    {
                        item.add(itemKey, defaultItem.get(itemKey).deepCopy());
                        updated = true;
                    }
                }
            }
        }

        final JsonObject defaultPayouts = defaults.getAsJsonObject("payouts");
        final JsonObject payouts = config.getAsJsonObject("payouts");
        if (payouts != null && defaultPayouts != null)
        {
            for (String key : defaultPayouts.keySet())
            {
                if (!payouts.has(key))
                {
                    payouts.add(key, defaultPayouts.get(key).deepCopy());
                    updated = true;
                }
            }
        }

        JsonObject heads = config.getAsJsonObject("heads");
        if (heads == null)
        {
            heads = new JsonObject();
            config.add("heads", heads);
        }

        for (LetterResult result : LetterResult.values())
        {
            final String groupName = result.name().toLowerCase(Locale.ROOT);
            final JsonObject defaultGroup = defaultHeads.getAsJsonObject(groupName);
            if (defaultGroup == null)
                throw new IllegalStateException("Default Wordle configuration is missing " + groupName + " head textures.");

            JsonObject group = heads.getAsJsonObject(groupName);
            if (group == null)
            {
                group = new JsonObject();
                heads.add(groupName, group);
            }

            for (char letter = 'A'; letter <= 'Z'; letter++)
            {
                final String key = String.valueOf(letter);
                if (!group.has(key))
                {
                    group.add(key, defaultGroup.get(key).deepCopy());
                    updated = true;
                }
            }
        }

        return updated;
    }

    private static void writeConfiguration(final Path path, final JsonObject config) throws IOException
    {
        final Path temporaryPath = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporaryPath, JsonUtil.GSON.toJson(config), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try
        {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException exception)
        {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeEmptyDirectory(final Path directory) throws IOException
    {
        if (!Files.isDirectory(directory))
            return;

        try (Stream<Path> paths = Files.list(directory))
        {
            if (paths.findAny().isEmpty())
                Files.delete(directory);
        }
    }
}
