package me.totalfreedom.totalfreedommod.eco;

/**
 * Persisted snapshot of the bank's own native account balances.
 */
public record EcoBankData(int checkingBalance, int savingsBalance)
{
}
