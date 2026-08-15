package me.totalfreedom.totalfreedommod.eco;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ITransaction;
import me.totalfreedom.api.economy.IWallet;
import me.totalfreedom.api.economy.TStatus;
import me.totalfreedom.api.economy.Transactional;

public final class Wallet implements IWallet
{
    private final IEcoPlayer player;

    private volatile int balance;

    public Wallet(final IEcoPlayer player)
    {
        this(player, 0);
    }

    Wallet(final IEcoPlayer player, final int initialBalance)
    {
        this.player = player;
        this.balance = initialBalance;
    }

    @Override
    public IEcoPlayer getPlayer()
    {
        return this.player;
    }

    @Override
    public int balance()
    {
        return this.balance;
    }

    void setBalance(final int balance)
    {
        this.balance = balance;
    }

    @Override
    public <S extends Transactional<S>> ITransaction<S, IWallet> deposit(final S sender, final int amount, final int tax)
    {
        if (amount <= 0)
            return new Transaction<>(sender, 
                                     this, 
                                     amount, 
                                     TStatus.INVALID_AMOUNT, 
                                     tax, 
                                     null);

        this.balance += amount;
        return new Transaction<>(sender, 
                                 this, 
                                 amount, 
                                 TStatus.SUCCESS, 
                                 tax, 
                                 null);
    }

    @Override
    public <R extends Transactional<R>> ITransaction<IWallet, R> withdraw(final R recipient, final int amount, final int tax)
    {
        if (amount <= 0)
            return new Transaction<>(this, 
                                     recipient, 
                                     amount, 
                                     TStatus.INVALID_AMOUNT, 
                                     tax, 
                                     null);

        if (this.balance < amount)
            return new Transaction<>(this, 
                                     recipient, 
                                     amount, 
                                     TStatus.INSUFFICIENT_FUNDS, 
                                     tax, 
                                     null);

        this.balance -= amount;

        if (recipient instanceof IEcoPlayer player)
            player.getWallet().deposit(this, amount, tax);
        else if (recipient instanceof IBank bank)
            bank.bankNative().deposit(this, amount, 0); // payments to the bank are never taxed
        else if (recipient instanceof IBankAccount<?> account)
            account.deposit(this, amount, tax);
        else if (recipient instanceof IWallet wallet)
            wallet.deposit(this, amount, tax);

        return new Transaction<>(this, 
                                 recipient, 
                                 amount, 
                                 TStatus.SUCCESS, 
                                 tax, 
                                 null);
    }

    @Override
    public ITransaction<IEcoPlayer, IEcoPlayer> pay(final IEcoPlayer recipient, final int amount)
    {
        if (amount <= 0)
            return new Transaction<>(this.player, 
                                     recipient, 
                                     amount, 
                                     TStatus.INVALID_AMOUNT, 
                                     0, 
                                     null);

        if (this.balance < amount)
            return new Transaction<>(this.player, 
                                     recipient, 
                                     amount, 
                                     TStatus.INSUFFICIENT_FUNDS, 
                                     0, 
                                     null);

        this.balance -= amount;
        recipient.getWallet().deposit(this.player, amount, 0);

        return new Transaction<>(this.player, recipient, amount, TStatus.SUCCESS, 0, null);
    }
}
