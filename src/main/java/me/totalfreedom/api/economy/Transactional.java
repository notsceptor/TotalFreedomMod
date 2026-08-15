package me.totalfreedom.api.economy;

/**
 * Represents any object that can receive money.
 */
public interface Transactional<T extends Transactional<T>>
{   
    <S extends Transactional<S>> ITransaction<S, T> deposit(final S sender, final int amount, final int tax);

    <R extends Transactional<R>> ITransaction<T, R> withdraw(final R recipient, final int amount, final int tax);
}
