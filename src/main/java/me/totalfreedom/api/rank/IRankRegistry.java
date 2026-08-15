package me.totalfreedom.api.rank;

import java.util.Optional;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.rank.CustomRank;

/**
 * The single place that answers "what rank is this?" and "what rank does this permission need?".
 */
public interface IRankRegistry
{
    /**
     * Rebuilds the permission index from the current rank map. Call after ranks are loaded or after
     * inheritance is re-resolved, on the main thread.
     */
    void reindex();

    /**
     * The rank registered under {@code id}, if any.
     */
    Optional<CustomRank> byId(String id);

    /**
     * The rank filling {@code role}, preferring one that declares it in {@code ranks.json} and
     * otherwise deriving it from the shape of the registry.
     */
    Optional<CustomRank> byRole(RankRole role);

    /**
     * The rank every unplaceable sender falls back to.
     */
    Optional<CustomRank> floor();

    /**
     * Whether the rank {@code rankId} sits at or above the rank filling {@code role}. An unknown
     * rank answers {@code false}, so a stale id never clears a floor it was not granted.
     */
    boolean isAtLeast(String rankId, RankRole role);

    /**
     * The least privileged rank that grants {@code permission}, which is the tier a command guarded
     * by that node actually requires. Empty when no rank grants it, meaning the node is unreachable.
     */
    Optional<CustomRank> requiredFor(String permission);

    /**
     * The rank {@code sender} acts at, falling back to {@link #floor()} when it cannot be placed.
     */
    Optional<CustomRank> forSender(CommandSender sender);

    /**
     * Whether {@code sender} may exercise {@code permission}, by rank or by title.
     */
    boolean satisfies(CommandSender sender, String permission);

    /**
     * Whether a rank sits at or above the tier {@code permission} requires.
     */
    boolean satisfies(CustomRank holder, String permission);
}
