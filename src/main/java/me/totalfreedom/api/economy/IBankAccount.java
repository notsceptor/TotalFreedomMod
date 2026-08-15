package me.totalfreedom.api.economy;

public interface IBankAccount<T extends Transactional<T>> extends Transactional<IBankAccount<T>>
{
    /**
     * @return either an {@link IEcoPlayer} (usually),
     * or the {@link IBank} if called from {@link IBank#bankNative()}
     */
    T owner();

    int checkingBalance();

    int savingsBalance();

    ITransaction<IBankAccount<T>, IBankAccount<T>> moveToSavings(final int amount);

    ITransaction<IBankAccount<T>, IBankAccount<T>> moveFromSavings(final int amount);

    /**
     * Wires money to the {@code recipient} from this {@code IBankAccount}.
     *
     * @param recipient
     * @param amount
     * @param tax the amount to tax to the Bank.
     * @return An {@link ITransaction} containing information about the transaction.
     */
    <R extends Transactional<R>> ITransaction<IBankAccount<T>, R> wire(final R recipient, final int amount, final int tax);
}
