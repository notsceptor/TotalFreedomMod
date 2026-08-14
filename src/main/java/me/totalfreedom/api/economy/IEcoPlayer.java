package me.totalfreedom.api.economy;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;

public interface IEcoPlayer extends Transactional
{
    /** The player's Minecraft username */
    String name();

    /** The player's Minecraft UUID */
    UUID uuid();

    /** The player's {@link IWallet}. */
    IWallet getWallet();

    /** The player's {@link IBankAccount}. */
    IBankAccount<IEcoPlayer> getBankAccount();

    /** An {@link Optional} containing the Bukkit {@link Player} object, if they're online */
    Optional<Player> bukkit();

    /** Quick wallet lookup */
    default int getWalletBalance()
    {
        return getWallet().balance();
    }

    /** Quick checking lookup */
    default int getCheckingBalance()
    {
        return getBankAccount().checkingBalance();
    }

    /** Quick savings lookup */
    default int getSavingsBalance()
    {
        return getBankAccount().savingsBalance();
    }
}
