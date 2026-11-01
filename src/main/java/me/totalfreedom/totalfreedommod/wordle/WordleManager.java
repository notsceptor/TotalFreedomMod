package me.totalfreedom.totalfreedommod.wordle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.gson.reflect.TypeToken;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.inventory.InventoryView;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.api.display.Displayable;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.cmd.MessageUtils;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FTask;
import me.totalfreedom.totalfreedommod.util.JsonUtil;
import me.totalfreedom.totalfreedommod.wordle.WordleGame.LetterResult;
import me.totalfreedom.totalfreedommod.wordle.WordlePlayerData.Status;

public final class WordleManager extends FreedomService
{
    private static final long SHUTDOWN_FLUSH_TIMEOUT_SECONDS = 10L;
    private static final int LEADERBOARD_ENTRIES_PER_PAGE = 4;
    private static final int BOARD_ROW_WIDTH = 9;
    private static final int BOARD_SIZE = WordleGame.MAX_GUESSES * BOARD_ROW_WIDTH;
    private static final int FIRST_LETTER_COLUMN = 2;
    private static final int HELP_SLOT = 1;
    private static final int STATS_SLOT = 7;
    private static final int LEADERBOARD_SLOT = 16;
    private static final int GUESS_SLOT = 19;
    private static final int CLOSE_SLOT = 52;
    private static final int ANVIL_INPUT_SLOT = 0;
    private static final int ANVIL_RESULT_SLOT = 2;
    private static final int PERCENT_MULTIPLIER = 100;
    private static final Type LEADERBOARD_TYPE = new TypeToken<List<WordleLeaderboardEntry>>() {}.getType();

    private final Map<UUID, WordlePlayerData> playerData = new HashMap<>();
    private final java.util.Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();
    private final ExecutorService persistenceExecutor = Executors.newSingleThreadExecutor(task ->
    {
        final Thread thread = new Thread(task, "TFM-Wordle-Persistence");
        thread.setDaemon(true);
        return thread;
    });

    private Path dataDirectory;
    private WordList wordList;
    private WordSelector wordSelector;
    private HeadCache headCache;
    private int baseWinPayout;
    private int unusedGuessPayout;

    public WordleManager(final FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        final Path pluginDirectory = plugin.getDataFolder().toPath();
        dataDirectory = pluginDirectory.resolve("fun").resolve("wordle");
        migrateLegacyData(pluginDirectory.resolve("eco").resolve("wordle"), dataDirectory);
        migrateLegacyData(pluginDirectory.resolve("wordle"), dataDirectory);
        wordList = new WordList(plugin);
        wordSelector = new DailyWordSelector(wordList);
        headCache = new HeadCache(wordList);
        baseWinPayout = wordList.baseWinPayout();
        unusedGuessPayout = wordList.unusedGuessPayout();
    }

    @Override
    public void onStop()
    {
        playerData.forEach(this::save);
        persistenceExecutor.shutdown();

        try
        {
            if (!persistenceExecutor.awaitTermination(SHUTDOWN_FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            {
                FLog.error("Wordle persistence did not finish before shutdown; pending saves may be lost.");
                persistenceExecutor.shutdownNow();
            }
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            FLog.error("Interrupted while waiting for Wordle persistence to finish.");
            FLog.error(exception);
        }
    }

    public void open(final Player player)
    {
        final UUID playerId = player.getUniqueId();
        final WordlePlayerData loaded = playerData.get(playerId);
        if (loaded != null)
        {
            final WordlePlayerData current = currentData(loaded);
            playerData.put(playerId, current);
            if (!current.equals(loaded))
                save(playerId, current);
            openBoard(player, current);
            return;
        }

        if (!loadingPlayers.add(playerId))
            return;

        CompletableFuture.supplyAsync(() -> load(playerId), persistenceExecutor)
                .whenComplete((data, failure) -> server.getScheduler().runTask(plugin,
                        FTask.guard("Wordle/loadPlayerData", () ->
                        {
                            loadingPlayers.remove(playerId);
                            if (!player.isOnline())
                                return;

                            if (failure != null)
                            {
                                FLog.error("Could not load Wordle data for " + player.getName() + ".");
                                FLog.error(failure);
                                player.sendMessage(MessageUtils.parse(wordList.message("load_failed")));
                                return;
                            }

                            final WordlePlayerData current = currentData(data);
                            playerData.put(playerId, current);
                            if (!current.equals(data))
                                save(playerId, current);
                            openBoard(player, current);
                        })));
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event)
    {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof WordleGui))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize())
            return;

        switch (event.getRawSlot())
        {
            case HELP_SLOT -> player.openBook(createHelpBook());
            case LEADERBOARD_SLOT -> openLeaderboard(player);
            case GUESS_SLOT -> openGuessAnvil(player);
            case CLOSE_SLOT -> player.closeInventory();
            default -> { }
        }
    }

    @EventHandler
    public void onPrepareAnvil(final PrepareAnvilEvent event)
    {
        if (!WordleInventoryHolder.hasAnvilOpen(event.getView().getPlayer().getUniqueId()))
            return;

        final String guess = event.getView().getRenameText();
        if (guess == null || guess.isBlank())
        {
            event.setResult(null);
            return;
        }

        event.setResult(namedItem(Material.PAPER, wordList.message("anvil_result_name"),
                wordList.messageLines("anvil_result_lore"),
                MessageUtils.unparsed("guess", guess)));
        event.getView().setRepairCost(0);
    }

    @EventHandler
    public void onWordleAnvilClick(final InventoryClickEvent event)
    {
        if (!WordleInventoryHolder.hasAnvilOpen(event.getWhoClicked().getUniqueId()))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()
                || event.getSlot() != ANVIL_RESULT_SLOT)
            return;

        if (!(event.getView() instanceof AnvilView anvilView))
            return;

        final String guess = anvilView.getRenameText();
        if (guess == null || !WordleGame.isValidWord(guess.toUpperCase(Locale.ROOT)))
        {
            player.sendMessage(MessageUtils.parse(wordList.message("invalid_anvil_guess")));
            return;
        }

        anvilView.getTopInventory().setItem(ANVIL_INPUT_SLOT, null);
        player.closeInventory();
        submitGuess(player, guess);
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        WordleInventoryHolder.clearAnvil(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event)
    {
        if (event.getView().getTopInventory().getHolder(false) instanceof WordleGui
                || WordleInventoryHolder.hasAnvilOpen(event.getWhoClicked().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        final UUID playerId = event.getPlayer().getUniqueId();
        final WordlePlayerData data = playerData.get(playerId);
        if (data != null)
            save(playerId, data);
    }

    private WordlePlayerData load(final UUID playerId)
    {
        final Path file = dataDirectory.resolve(playerId + ".json");
        if (!Files.exists(file))
            return emptyData();

        try
        {
            final String json = Files.readString(file, StandardCharsets.UTF_8);
            final WordlePlayerData data = JsonUtil.GSON.fromJson(json, WordlePlayerData.class);
            validate(data);
            return data;
        }
        catch (IOException exception)
        {
            throw new UncheckedIOException(exception);
        }
        catch (RuntimeException exception)
        {
            throw new IllegalStateException("Invalid Wordle data file for " + playerId, exception);
        }
    }

    private void save(final UUID playerId, final WordlePlayerData data)
    {
        final String json = JsonUtil.GSON.toJson(data);
        final Runnable saveTask = () -> writePlayerData(playerId, json);

        if (persistenceExecutor.isShutdown())
        {
            saveTask.run();
            return;
        }

        try
        {
            persistenceExecutor.execute(saveTask);
        }
        catch (RejectedExecutionException exception)
        {
            saveTask.run();
        }
    }

    private void writePlayerData(final UUID playerId, final String json)
    {
        try
        {
            writeAtomically(dataDirectory.resolve(playerId + ".json"), json);
        }
        catch (IOException exception)
        {
            FLog.error("Could not save Wordle data for " + playerId + ".");
            FLog.error(exception);
        }
    }

    private void writeAtomically(final Path file, final String content) throws IOException
    {
        Files.createDirectories(file.getParent());
        final Path temporaryFile = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporaryFile, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try
        {
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException exception)
        {
            Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void migrateLegacyData(final Path legacyDirectory, final Path dataDirectory)
    {
        if (!Files.isDirectory(legacyDirectory))
            return;

        try (Stream<Path> paths = Files.walk(legacyDirectory))
        {
            for (Path source : paths.toList())
            {
                final Path target = dataDirectory.resolve(legacyDirectory.relativize(source));
                if (Files.isDirectory(source))
                {
                    Files.createDirectories(target);
                    continue;
                }

                if (Files.exists(target))
                    continue;

                Files.createDirectories(target.getParent());
                Files.move(source, target);
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not migrate legacy Wordle data from " + legacyDirectory, exception);
        }

        try (Stream<Path> paths = Files.walk(legacyDirectory))
        {
            for (Path directory : paths.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).toList())
            {
                try
                {
                    Files.delete(directory);
                }
                catch (DirectoryNotEmptyException ignored)
                {
                }
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not clean up migrated Wordle data at " + legacyDirectory, exception);
        }
    }

    private void validate(final WordlePlayerData data)
    {
        if (data == null || data.gameDate() == null || data.guesses() == null || data.status() == null
                || data.lastWinDate() == null || data.guesses().size() > WordleGame.MAX_GUESSES
                || data.currentStreak() < 0 || data.gamesPlayed() < 0 || data.gamesWon() < 0
                || data.gamesWon() > data.gamesPlayed())
            throw new IllegalStateException("Wordle save data has invalid fields.");

        if (!data.gameDate().isEmpty())
            LocalDate.parse(data.gameDate());

        if (!data.lastWinDate().isEmpty())
            LocalDate.parse(data.lastWinDate());

        if (data.guesses().stream().anyMatch(guess -> !WordleGame.isValidWord(guess)))
            throw new IllegalStateException("Wordle save data contains an invalid guess.");
    }

    private WordlePlayerData currentData(final WordlePlayerData data)
    {
        final LocalDate today = LocalDate.now();
        if (today.toString().equals(data.gameDate()))
            return data;

        final int streak = data.lastWinDate().equals(today.minusDays(1).toString()) ? data.currentStreak() : 0;
        return new WordlePlayerData(today.toString(), List.of(), Status.IN_PROGRESS,
                data.lastWinDate(), streak, data.gamesPlayed(), data.gamesWon());
    }

    private void openBoard(final Player player, final WordlePlayerData data)
    {
        final WordleGui holder = new WordleGui();
        final Inventory inventory = Bukkit.createInventory(holder, BOARD_SIZE,
                MessageUtils.parse(wordList.message("board_title")));
        holder.setInventory(inventory);

        for (int slot = 0; slot < inventory.getSize(); slot++)
            inventory.setItem(slot, namedItem(Material.BLACK_STAINED_GLASS_PANE, "", List.of()));

        final LocalDate gameDate = LocalDate.parse(data.gameDate());
        final String answer = wordSelector.select(gameDate);
        for (int row = 0; row < WordleGame.MAX_GUESSES; row++)
        {
            final Material blankMaterial = row == data.guesses().size() && data.status() == Status.IN_PROGRESS
                    ? Material.WHITE_STAINED_GLASS_PANE : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            final List<LetterResult> scored = row < data.guesses().size()
                    ? WordleGame.score(answer, data.guesses().get(row)) : List.of();

            for (int column = 0; column < WordleGame.WORD_LENGTH; column++)
            {
                final int slot = row * BOARD_ROW_WIDTH + FIRST_LETTER_COLUMN + column;
                if (row < data.guesses().size())
                    inventory.setItem(slot, headCache.get(scored.get(column), data.guesses().get(row).charAt(column)));
                else
                    inventory.setItem(slot, namedItem(blankMaterial, "", List.of()));
            }
        }

        inventory.setItem(HELP_SLOT, menuItem("how_to_play"));
        inventory.setItem(STATS_SLOT, statsItem(data));
        inventory.setItem(LEADERBOARD_SLOT, menuItem("leaderboard"));
        inventory.setItem(GUESS_SLOT, menuItem(data.status() == Status.IN_PROGRESS ? "guess" : "game_complete"));
        inventory.setItem(CLOSE_SLOT, menuItem("close"));

        player.openInventory(inventory);
    }

    private ItemStack createHelpBook()
    {
        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        final BookMeta meta = (BookMeta)book.getItemMeta();
        meta.setTitle(wordList.message("help_book_title"));
        meta.setAuthor("TotalFreedom");
        meta.pages(wordList.helpPages().stream()
                .map(this::bookPage)
                .toList());
        book.setItemMeta(meta);
        return book;
    }

    private Component bookPage(final WordleHelpPage helpPage)
    {
        final StringBuilder markup = new StringBuilder("<dark_gray><heading></dark_gray>\n\n");
        final List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(MessageUtils.unparsed("heading", interpolateHelpText(helpPage.heading())));
        for (int index = 0; index < helpPage.paragraphs().size(); index++)
        {
            if (index > 0)
                markup.append("\n\n");
            final String paragraphKey = "paragraph" + index;
            final String color = index % 2 == 0 ? "black" : "dark_gray";
            markup.append('<').append(color).append("><").append(paragraphKey).append("></").append(color).append('>');
            resolvers.add(MessageUtils.unparsed(paragraphKey,
                    interpolateHelpText(helpPage.paragraphs().get(index))));
        }
        return MessageUtils.parse(markup.toString(), resolvers.toArray(TagResolver[]::new));
    }

    private String interpolateHelpText(final String text)
    {
        final int firstGuessPayout = baseWinPayout
                + (WordleGame.MAX_GUESSES - 1) * unusedGuessPayout;
        return text.replace("{max_guesses}", Integer.toString(WordleGame.MAX_GUESSES))
                .replace("{base_payout}", Integer.toString(baseWinPayout))
                .replace("{unused_guess_bonus}", Integer.toString(unusedGuessPayout))
                .replace("{first_guess_payout}", Integer.toString(firstGuessPayout));
    }

    private void openGuessAnvil(final Player player)
    {
        final WordlePlayerData data = playerData.get(player.getUniqueId());
        if (data == null || data.status() != Status.IN_PROGRESS)
            return;

        final InventoryView view = player.openAnvil(null, true);
        view.setTitle(wordList.message("anvil_title"));
        view.setItem(ANVIL_INPUT_SLOT, namedItem(Material.PAPER, wordList.message("anvil_input_name"),
                wordList.messageLines("anvil_input_lore")));
        WordleInventoryHolder.markAnvilOpen(player.getUniqueId());
    }

    private void openLeaderboard(final Player player)
    {
        final LocalDate today = LocalDate.now();
        CompletableFuture.supplyAsync(() -> loadLeaderboard(today), persistenceExecutor)
                .whenComplete((entries, failure) -> server.getScheduler().runTask(plugin,
                        FTask.guard("Wordle/openLeaderboard", () ->
                        {
                            if (!player.isOnline())
                                return;

                            if (failure != null)
                            {
                                FLog.error("Could not load the Wordle leaderboard for " + today + ".");
                                FLog.error(failure);
                                player.sendMessage(MessageUtils.parse(wordList.message("leaderboard_load_failed")));
                                return;
                            }

                            player.openBook(createLeaderboardBook(today, entries));
                        })));
    }

    private List<WordleLeaderboardEntry> loadLeaderboard(final LocalDate date)
    {
        final Path file = leaderboardFile(date);
        if (!Files.exists(file))
            return List.of();

        try
        {
            final String json = Files.readString(file, StandardCharsets.UTF_8);
            final List<WordleLeaderboardEntry> entries = JsonUtil.GSON.fromJson(json, LEADERBOARD_TYPE);
            if (entries == null)
                throw new IllegalStateException("Wordle leaderboard file contains no data: " + file);

            entries.forEach(this::validateLeaderboardEntry);
            return entries.stream()
                    .sorted(leaderboardOrder())
                    .toList();
        }
        catch (IOException exception)
        {
            throw new UncheckedIOException(exception);
        }
    }

    private void recordLeaderboardResult(final Player player, final WordlePlayerData data, final boolean won)
    {
        final LocalDate date = LocalDate.parse(data.gameDate());
        final WordleLeaderboardEntry result = new WordleLeaderboardEntry(
                player.getUniqueId(), player.getName(), data.guesses().size(), won);

        persistenceExecutor.execute(() ->
        {
            final List<WordleLeaderboardEntry> entries = new ArrayList<>(loadLeaderboard(date));
            entries.removeIf(entry -> entry.playerId().equals(result.playerId()));
            entries.add(result);
            saveLeaderboard(date, entries);
        });
    }

    private void saveLeaderboard(final LocalDate date, final List<WordleLeaderboardEntry> entries)
    {
        try
        {
            writeAtomically(leaderboardFile(date), JsonUtil.GSON.toJson(entries));
        }
        catch (IOException exception)
        {
            FLog.error("Could not save the Wordle leaderboard for " + date + ".");
            FLog.error(exception);
        }
    }

    private Path leaderboardFile(final LocalDate date)
    {
        return dataDirectory.resolve("leaderboards").resolve(date + ".json");
    }

    private void validateLeaderboardEntry(final WordleLeaderboardEntry entry)
    {
        if (entry == null || entry.playerId() == null || entry.playerName() == null || entry.playerName().isBlank()
                || entry.guesses() < 1 || entry.guesses() > WordleGame.MAX_GUESSES)
            throw new IllegalStateException("Wordle leaderboard contains an invalid entry.");
    }

    private static Comparator<WordleLeaderboardEntry> leaderboardOrder()
    {
        return Comparator.comparing(WordleLeaderboardEntry::won).reversed()
                .thenComparingInt(WordleLeaderboardEntry::guesses)
                .thenComparing(WordleLeaderboardEntry::playerName, String.CASE_INSENSITIVE_ORDER);
    }

    private ItemStack createLeaderboardBook(final LocalDate date, final List<WordleLeaderboardEntry> entries)
    {
        final ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        final BookMeta meta = (BookMeta)book.getItemMeta();
        meta.setTitle(wordList.message("leaderboard_book_title"));
        meta.setAuthor("TotalFreedom");

        if (entries.isEmpty())
        {
            meta.pages(List.of(MessageUtils.parse(leaderboardHeader()
                    + "<dark_gray>" + wordList.message("leaderboard_empty") + "</dark_gray>",
                    MessageUtils.unparsed("date", date.toString()))));
        }
        else
        {
            final List<Component> pages = new ArrayList<>();
            for (int pageStart = 0; pageStart < entries.size(); pageStart += LEADERBOARD_ENTRIES_PER_PAGE)
            {
                final StringBuilder markup = new StringBuilder(leaderboardHeader());
                final List<TagResolver> resolvers = new ArrayList<>();
                resolvers.add(MessageUtils.unparsed("date", date.toString()));
                final int pageEnd = Math.min(pageStart + LEADERBOARD_ENTRIES_PER_PAGE, entries.size());

                for (int index = pageStart; index < pageEnd; index++)
                {
                    final WordleLeaderboardEntry entry = entries.get(index);
                    final NamedTextColor nameColor = leaderboardNameColor(entry.playerId(),
                            (index % 2 == 0) ? NamedTextColor.BLACK : NamedTextColor.DARK_GRAY);
                    final String playerKey = "player" + index;
                    final String rankKey = "rank" + index;
                    final String guessesKey = "guesses" + index;
                    final String squaresKey = "squares" + index;
                    markup.append("\n<black><").append(rankKey).append(">. </black><").append(playerKey)
                            .append(">\n<dark_gray>   </dark_gray><").append(squaresKey)
                            .append(">  <black><").append(guessesKey).append(">/")
                            .append(WordleGame.MAX_GUESSES).append("\n</black>");
                    resolvers.add(MessageUtils.component(playerKey, Component.text(entry.playerName(), nameColor)));
                    resolvers.add(MessageUtils.unparsed(rankKey, Integer.toString(index + 1)));
                    resolvers.add(MessageUtils.unparsed(guessesKey, Integer.toString(entry.guesses())));
                    resolvers.add(MessageUtils.component(squaresKey, MessageUtils.parse(guessSquares(entry))));
                }

                pages.add(MessageUtils.parse(markup.toString(), resolvers.toArray(TagResolver[]::new)));
            }

            meta.pages(pages);
        }

        book.setItemMeta(meta);
        return book;
    }

    private String leaderboardHeader()
    {
        return wordList.message("leaderboard_header");
    }

    private static String guessSquares(final WordleLeaderboardEntry entry)
    {
        final StringBuilder squares = new StringBuilder();
        for (int guess = 1; guess <= WordleGame.MAX_GUESSES; guess++)
        {
            final boolean usedGuess = guess <= entry.guesses();
            final String square = usedGuess ? "■" : "□";
            if (usedGuess && entry.won())
                squares.append("<green>").append(square).append("</green>");
            else
                squares.append("<dark_gray>").append(square).append("</dark_gray>");
        }
        return squares.toString();
    }

    private NamedTextColor leaderboardNameColor(final UUID playerId, final NamedTextColor fallback)
    {
        final Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (onlinePlayer != null)
        {
            final Displayable display = plugin.ranks().getDisplay(onlinePlayer);
            return display == null ? fallback : display.getColor();
        }

        final PlayerData data = plugin.players().getData(playerId);
        if (data == null)
            return fallback;

        if (plugin.titles() != null)
        {
            final NamedTextColor titleColor = plugin.titles().resolve(data.getTitles())
                    .stream()
                    .filter(me.totalfreedom.totalfreedommod.title.Title::isAnnounce)
                    .findFirst()
                    .map(me.totalfreedom.totalfreedommod.title.Title::getColor)
                    .orElse(null);
            if (titleColor != null)
                return titleColor;
        }

        if (data.isActiveStaff())
        {
            return data.getRankId()
                    .map(plugin.ranks()::getCustomRank)
                    .map(rank -> rank.getColor())
                    .orElse(fallback);
        }

        return fallback;
    }

    private void submitGuess(final Player player, final String rawGuess)
    {
        if (!player.isOnline())
            return;

        final UUID playerId = player.getUniqueId();
        final WordlePlayerData previous = playerData.get(playerId);
        if (previous == null)
            return;

        final WordlePlayerData current = currentData(previous);
        playerData.put(playerId, current);
        if (current.status() != Status.IN_PROGRESS)
        {
            openBoard(player, current);
            return;
        }

        final String guess = rawGuess.strip().toUpperCase(Locale.ROOT);
        if (!WordleGame.isValidWord(guess))
        {
            player.sendMessage(MessageUtils.parse(wordList.message("invalid_guess")));
            openGuessAnvil(player);
            return;
        }

        final List<String> guesses = new ArrayList<>(current.guesses());
        guesses.add(guess);
        final String answer = wordSelector.select(LocalDate.parse(current.gameDate()));
        final boolean won = guess.equals(answer);
        final boolean lost = !won && guesses.size() == WordleGame.MAX_GUESSES;
        final Status status = won ? Status.WON : lost ? Status.LOST : Status.IN_PROGRESS;
        final WordlePlayerData updated = new WordlePlayerData(current.gameDate(), guesses, status,
                won ? current.gameDate() : current.lastWinDate(),
                won ? current.currentStreak() + 1 : lost ? 0 : current.currentStreak(),
                won || lost ? current.gamesPlayed() + 1 : current.gamesPlayed(),
                won ? current.gamesWon() + 1 : current.gamesWon());

        playerData.put(playerId, updated);
        save(playerId, updated);
        if (won || lost)
            recordLeaderboardResult(player, updated, won);

        if (won)
            finishWin(player, updated);
        else if (lost)
            finishLoss(player, answer);
        else
            openBoard(player, updated);
    }

    private void finishWin(final Player player, final WordlePlayerData data)
    {
        final int payout = baseWinPayout
                + (WordleGame.MAX_GUESSES - data.guesses().size()) * unusedGuessPayout;
        final IEcoPlayer ecoPlayer = plugin.economy().getEcoPlayer(player);
        ecoPlayer.deposit(plugin.bank(), payout, 0);

        final TagResolver[] resolvers =
        {
            MessageUtils.unparsed("guesses", Integer.toString(data.guesses().size())),
            MessageUtils.unparsed("payout", Integer.toString(payout))
        };
        player.showTitle(Title.title(MessageUtils.parse(wordList.message("win_title"), resolvers),
                MessageUtils.parse(wordList.message("win_subtitle"), resolvers)));
        player.sendMessage(MessageUtils.parse(wordList.message("win_chat"), resolvers));
        openBoard(player, data);
    }

    private void finishLoss(final Player player, final String answer)
    {
        final TagResolver answerResolver = MessageUtils.unparsed("answer", answer);
        player.showTitle(Title.title(MessageUtils.parse(wordList.message("loss_title")),
                MessageUtils.parse(wordList.message("loss_subtitle"), answerResolver)));
        player.sendMessage(MessageUtils.parse(wordList.message("loss_chat"), answerResolver));
        openBoard(player, playerData.get(player.getUniqueId()));
    }

    private ItemStack statsItem(final WordlePlayerData data)
    {
        final double winRate = data.gamesPlayed() == 0 ? 0.0
                : (double)data.gamesWon() * PERCENT_MULTIPLIER / data.gamesPlayed();
        return menuItem("stats",
                MessageUtils.unparsed("current_streak", Integer.toString(data.currentStreak())),
                MessageUtils.unparsed("win_rate", String.format(Locale.ROOT, "%.0f", winRate)),
                MessageUtils.unparsed("games_played", Integer.toString(data.gamesPlayed())));
    }

    private ItemStack menuItem(final String name, final TagResolver... resolvers)
    {
        final WordList.MenuItem item = wordList.interactable(name);
        return namedItem(item.material(), item.name(), item.lore(), resolvers);
    }

    private static ItemStack namedItem(final Material material, final String name,
            final List<String> lore, final TagResolver... resolvers)
    {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtils.parse(name, resolvers));
        meta.lore(lore.stream()
                .map(line -> MessageUtils.parse(line, resolvers))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private static WordlePlayerData emptyData()
    {
        return new WordlePlayerData("", List.of(), Status.IN_PROGRESS, "", 0, 0, 0);
    }
}
