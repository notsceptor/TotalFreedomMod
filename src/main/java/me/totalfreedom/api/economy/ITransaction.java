package me.totalfreedom.api.economy;

import java.util.Optional;

public interface ITransaction<S extends Transactional<S>, R extends Transactional<R>>
{
    /**
     * This returns who paid out the transaction.
     * <p>
     * This should almost always exclusively return {@link IEcoPlayer}, except for when
     * the {@link IBank} itself pays out the player.
     */
    S sender();

    /**
     * This returns who receives the transaction.
     * <p>
     * In the case of self-transaction (i.e. Player deposits from wallet to bank), then
     * the receiver should be the {@link IWallet} or {@link IBankAccount}, not the {@link IBank} or
     * another {@link IEcoPlayer}.
     * <p>
     * Transactions like loan repayments can result in the {@link IBank} being the receiver, 
     * but note that any transaction to the {@code IBank} will deposit directly to 
     * the {@link IBank#bankNative()}.
     * <p>
     * Obviously, sending to another player would dictate that the recipient {@link IEcoPlayer} 
     * is what this should return.
     */
    R recipient();

    /**
     * For failed transactions, this remains the requested amount, taxes are always 0, 
     * and neither balance changes. 
     * 
     * When taxed, assuming {@code 0 <= taxedAmount() <= amount()}, the receiver gets 
     * {@code amount() - taxedAmount()} and the {@link IBank#bankNative()} gets the {@code taxedAmount()}.
     * 
     * @return The amount of money handled in this transaction. 
     */
    int amount();

    /**
     * This enum will either return {@link TStatus#SUCCESS} if it completed,
     * {@link TStatus#INVALID_AMOUNT} if the amount was unsupported {@code (0, negative)},
     * or {@link TStatus#INSUFFICIENT_FUNDS} if the sender doesn't have enough money to complete the transaction.
     */
    TStatus status();

    /**
     * Represents the Bank, in case there was tax.
     * 
     * @return an empty Optional if {@link #taxedAmount()} is 0, otherwise returns the {@link IBank}
     */
    Optional<IBank> taxRecipient();

    /**
     * @return 0 if no tax, otherwise number deducted from transaction (taxes).
     */
    int taxAmount();
}
