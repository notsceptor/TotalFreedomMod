package me.totalfreedom.api.economy;

public interface IBankAccount<T extends Transactional> extends Transactional
{
    /**
     * @return either an {@link IEcoPlayer} (usually), 
     * or the {@link IBank} if called from {@link IBank#bankNative()}
     */
    T owner();
    
    int checkingBalance();

    int savingsBalance();

    /**
     * Deposits into this bank account. 
     * Deposits always target the checking account.
     * 
     * @param amount The amount to deposit
     * @return An immutable transaction instance that represents this deposit.
     */
    ITransaction<T, IBankAccount<T>> deposit(final int amount);
    
    /**
     * Withdraws from this {@code IBankAccount} to the associated {@link IEcoPlayer}'s {@link IWallet}
     * 
     * @param amount
     * @return
    */
    ITransaction<IBankAccount<T>, T> withdraw(final int amount);

    ITransaction<IBankAccount<T>, IBankAccount<T>> moveToSavings(final int amount);

    ITransaction<IBankAccount<T>, IBankAccount<T>> moveFromSavings(final int amount);

    /**
     * Wires money to the {@code recipient} from this {@code IBankAccount}.
     * 
     * @param recipient
     * @param amount
     * @return An {@link ITransaction} containing information about the transaction.
     */
    ITransaction<T, IBankAccount<? extends Transactional>> wire(final Transactional recipient, final int amount);
}
