package me.totalfreedom.totalfreedommod.eco;

import java.util.UUID;

/**
 * Persisted snapshot of one player's economy balances.
 */
public record EcoPlayerData(String username, UUID uuid, int walletBalance, int checkingBalance, int savingsBalance)
{
}
