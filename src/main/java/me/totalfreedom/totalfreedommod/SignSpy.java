package me.totalfreedom.totalfreedommod;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.papermc.paper.math.Position;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Displayable;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FTask;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SignSpy extends FreedomService
{
    private static final int LINES_PER_SIDE = 4;
    private static final Duration VIEW_LIFETIME = Duration.ofMinutes(10);
    // The client closes the sign editor on its own if the faked block entity disappears under it,
    // so the revert doubles as a force-close deadline rather than running right after opening.
    private static final long REVERT_DELAY_TICKS = 20L * 60L;
    private static final int FAKE_SIGN_DEPTH = 4;

    private Map<UUID, Location> pendingReverts;

    private record SignSnapshot(BlockData blockData, Side side,
                                List<Component> lines, DyeColor color, boolean glowing,
                                List<Component> otherLines, DyeColor otherColor, boolean otherGlowing)
    {
    }

    private static Side opposite(final Side side)
    {
        return side == Side.FRONT ? Side.BACK : Side.FRONT;
    }

    public SignSpy(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        this.pendingReverts = new HashMap<>();
    }

    @Override
    protected void onStop()
    {
        for (final UUID viewerId : List.copyOf(pendingReverts.keySet()))
        {
            revertPending(viewerId);
        }
        this.pendingReverts = null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event)
    {
        final Player editor = event.getPlayer();

        // The event fires before the edit is applied, so the block state still holds the
        // pre-edit text; diff against it so the chat line shows what actually changed.
        final Sign oldState = event.getBlock().getState() instanceof Sign sign ? sign : null;
        final SignSide oldSide = oldState != null ? oldState.getSide(event.getSide()) : null;

        String changedLine = "";
        String firstLine = "";
        int nonEmptyLines = 0;
        boolean changed = false;
        for (int i = 0; i < LINES_PER_SIDE; i++)
        {
            final Component line = event.line(i);
            final String plain = line != null ? AdventureUtil.componentToPlainText(line).trim() : "";
            final String oldPlain = oldSide != null
                    ? AdventureUtil.componentToPlainText(oldSide.line(i)).trim() : "";
            if (!plain.equals(oldPlain))
            {
                changed = true;
                if (changedLine.isEmpty() && !plain.isEmpty())
                {
                    changedLine = plain;
                }
            }
            if (plain.isEmpty())
            {
                continue;
            }
            if (nonEmptyLines == 0)
            {
                firstLine = plain;
            }
            nonEmptyLines++;
        }

        // Covers no-op edits and blank placements alike: submitting the editor without altering
        // any line is not worth logging.
        if (!changed)
        {
            return;
        }

        final String displayLine = !changedLine.isEmpty() ? changedLine : firstLine;

        int otherNonEmptyLines = 0;
        if (oldState != null)
        {
            final SignSide otherSide = oldState.getSide(opposite(event.getSide()));
            for (int i = 0; i < LINES_PER_SIDE; i++)
            {
                if (!AdventureUtil.componentToPlainText(otherSide.line(i)).trim().isEmpty())
                {
                    otherNonEmptyLines++;
                }
            }
        }

        final String sideName = event.getSide() == Side.FRONT ? "front" : "back";
        Component message = Component.empty();
        if (plugin.al.isAdmin(editor))
        {
            final Displayable display = plugin.rm.getDisplay(editor);
            String prefix = AdventureUtil.componentToPlainText(display.getColoredTag()).trim();
            if (prefix.isEmpty())
            {
                final String tag = display.getTag();
                prefix = tag != null ? tag : "";
            }
            if (!prefix.isEmpty())
            {
                message = Component.text(prefix + " ", display.getColor());
            }
        }
        message = message.append(Component.text(
                editor.getName() + " edited sign (" + sideName + "): '" + displayLine + "'",
                NamedTextColor.GRAY));
        if (otherNonEmptyLines > 0)
        {
            final SignSnapshot snapshot = snapshot(event, oldState);
            message = message.append(Component.text(" [View: ", NamedTextColor.GRAY))
                    .append(viewButton("Front", "Click to view the front of the sign",
                            snapshot, Side.FRONT))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(viewButton("Back", "Click to view the back of the sign",
                            snapshot, Side.BACK))
                    .append(Component.text("]", NamedTextColor.GRAY));
        }
        else if (nonEmptyLines > 1)
        {
            final SignSnapshot snapshot = snapshot(event, oldState);
            message = message.append(viewButton(" [See more]", "Click to view the full sign",
                    snapshot, snapshot.side()));
        }

        for (final Player admin : plugin.al.getOnlineAdmins())
        {
            if (admin.equals(editor))
            {
                continue;
            }
            final PlayerData data = plugin.pl.getData(admin);
            if (data == null || !data.isSignSpy())
            {
                continue;
            }
            FUtil.playerMsg(admin, message);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        if (pendingReverts != null)
        {
            pendingReverts.remove(event.getPlayer().getUniqueId());
        }
    }

    private SignSnapshot snapshot(final SignChangeEvent event, final Sign oldState)
    {
        DyeColor color = DyeColor.BLACK;
        boolean glowing = false;
        DyeColor otherColor = DyeColor.BLACK;
        boolean otherGlowing = false;
        final List<Component> otherLines = new ArrayList<>(LINES_PER_SIDE);
        // Dye and glow are applied by separate interactions, never by the edit itself, so the
        // pre-edit state is the correct source for them; only the text comes from the event.
        if (oldState != null)
        {
            final SignSide side = oldState.getSide(event.getSide());
            color = side.getColor() != null ? side.getColor() : DyeColor.BLACK;
            glowing = side.isGlowingText();
            final SignSide otherSide = oldState.getSide(opposite(event.getSide()));
            otherColor = otherSide.getColor() != null ? otherSide.getColor() : DyeColor.BLACK;
            otherGlowing = otherSide.isGlowingText();
            for (int i = 0; i < LINES_PER_SIDE; i++)
            {
                otherLines.add(otherSide.line(i));
            }
        }
        while (otherLines.size() < LINES_PER_SIDE)
        {
            otherLines.add(Component.empty());
        }

        final List<Component> lines = new ArrayList<>(LINES_PER_SIDE);
        for (int i = 0; i < LINES_PER_SIDE; i++)
        {
            final Component line = event.line(i);
            lines.add(line != null ? line : Component.empty());
        }

        return new SignSnapshot(event.getBlock().getBlockData().clone(), event.getSide(),
                List.copyOf(lines), color, glowing, List.copyOf(otherLines), otherColor, otherGlowing);
    }

    /**
     * A callback click event reaches the server as an opaque payload rather than a command, so
     * unlike runCommand it needs no client-side command parse and never prompts the click
     * confirmation screen added in 1.21.6.
     */
    private Component viewButton(final String label, final String hover,
                                 final SignSnapshot snapshot, final Side displaySide)
    {
        return Component.text(label, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.callback(
                        audience ->
                        {
                            if (audience instanceof Player viewer)
                            {
                                Bukkit.getScheduler().runTask(plugin, FTask.guard("SignSpy/openView",
                                        () -> openView(viewer, snapshot, displaySide)));
                            }
                        },
                        ClickCallback.Options.builder()
                                .uses(ClickCallback.UNLIMITED_USES)
                                .lifetime(VIEW_LIFETIME)
                                .build()))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    private void openView(final Player viewer, final SignSnapshot snapshot, final Side displaySide)
    {
        if (!viewer.isOnline())
        {
            return;
        }

        revertPending(viewer.getUniqueId());

        final World world = viewer.getWorld();
        final Location loc = viewer.getLocation().toBlockLocation();
        loc.setY(Math.max(world.getMinHeight(), loc.getBlockY() - FAKE_SIGN_DEPTH));

        final BlockData blockData = snapshot.blockData();
        final Sign state = (Sign) blockData.createBlockState();
        final SignSide sideState = state.getSide(snapshot.side());
        final SignSide otherSideState = state.getSide(opposite(snapshot.side()));
        for (int i = 0; i < LINES_PER_SIDE; i++)
        {
            sideState.line(i, snapshot.lines().get(i));
            otherSideState.line(i, snapshot.otherLines().get(i));
        }
        sideState.setColor(snapshot.color());
        sideState.setGlowingText(snapshot.glowing());
        otherSideState.setColor(snapshot.otherColor());
        otherSideState.setGlowingText(snapshot.otherGlowing());

        viewer.sendBlockChange(loc, blockData);
        viewer.sendBlockUpdate(loc, state);
        viewer.openVirtualSign(Position.block(loc), displaySide);

        pendingReverts.put(viewer.getUniqueId(), loc);
        Bukkit.getScheduler().runTaskLater(plugin,
                FTask.guard("SignSpy/revertFakeSign", () -> revertIfCurrent(viewer.getUniqueId(), loc)),
                REVERT_DELAY_TICKS);
    }

    private void revertIfCurrent(final UUID viewerId, final Location faked)
    {
        if (pendingReverts != null && faked.equals(pendingReverts.get(viewerId)))
        {
            revertPending(viewerId);
        }
    }

    private void revertPending(final UUID viewerId)
    {
        final Location faked = pendingReverts.remove(viewerId);
        if (faked == null)
        {
            return;
        }
        final Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer == null || !viewer.getWorld().equals(faked.getWorld()))
        {
            // A world change forces a full chunk resend, which cleans the ghost block up anyway.
            return;
        }
        viewer.sendBlockChange(faked, faked.getBlock().getBlockData());
        if (faked.getBlock().getState() instanceof TileState tile)
        {
            viewer.sendBlockUpdate(faked, tile);
        }
    }
}
