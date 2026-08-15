package me.totalfreedom.totalfreedommod.eco;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ITransaction;
import me.totalfreedom.api.economy.IWallet;
import me.totalfreedom.api.economy.TStatus;
import me.totalfreedom.api.economy.Transactional;

public class BankAccount<T extends Transactional<T>> implements IBankAccount<T>
{
    private final T owner;

    private volatile int checking;
    private volatile int savings;

    public BankAccount(final T owner)
    {
        this(owner, 0, 0);
    }

    BankAccount(final T owner, final int initialChecking, final int initialSavings)
    {
        this.owner = owner;
        this.checking = initialChecking;
        this.savings = initialSavings;
    }

    @Override
    public T owner()
    {
        return this.owner;
    }

    @Override
    public int checkingBalance()
    {
        return this.checking;
    }

    @Override
    public int savingsBalance()
    {
        return this.savings;
    }

    int updateChecking(final int balance)
    {
        this.checking = balance;
        return this.checking;
    }
    
    int updateSavings(final int balance)
    {
        this.savings = balance;
        return this.savings;
    }

    @Override
    public <S extends Transactional<S>> ITransaction<S, IBankAccount<T>> deposit(final S sender, final int amount, final int tax)
    {
        if (amount <= 0)
            return new Transaction<>(sender, this, amount, TStatus.INVALID_AMOUNT, tax, null);

        this.checking += amount;
        return new Transaction<>(sender, this, amount, TStatus.SUCCESS, tax, null);
    }

    @Override
    public <R extends Transactional<R>> ITransaction<IBankAccount<T>, R> withdraw(final R recipient, final int amount, final int tax)
    {
        return wire(recipient, amount, tax);
    }

    @Override
    public ITransaction<IBankAccount<T>, IBankAccount<T>> moveToSavings(final int amount)
    {
        if (amount <= 0)
            return new Transaction<>(this, this, amount, TStatus.INVALID_AMOUNT, 0, null);

        if (this.checking < amount)
            return new Transaction<>(this, this, amount, TStatus.INSUFFICIENT_FUNDS, 0, null);

        this.checking -= amount;
        this.savings += amount;
        return new Transaction<>(this, this, amount, TStatus.SUCCESS, 0, null);
    }

    @Override
    public ITransaction<IBankAccount<T>, IBankAccount<T>> moveFromSavings(final int amount)
    {
        if (amount <= 0)
            return new Transaction<>(this, this, amount, TStatus.INVALID_AMOUNT, 0, null);

        if (this.savings < amount)
            return new Transaction<>(this, this, amount, TStatus.INSUFFICIENT_FUNDS, 0, null);

        this.savings -= amount;
        this.checking += amount;
        return new Transaction<>(this, this, amount, TStatus.SUCCESS, 0, null);
    }

    @Override
    public <R extends Transactional<R>> ITransaction<IBankAccount<T>, R> wire(final R recipient, final int amount, final int tax)
    {
        if (amount <= 0)
            return new Transaction<>(this, recipient, amount, TStatus.INVALID_AMOUNT, tax, null);

        if (this.checking < amount)
            return new Transaction<>(this, recipient, amount, TStatus.INSUFFICIENT_FUNDS, tax, null);

        this.checking -= amount;

        if (recipient instanceof IEcoPlayer player)
            player.getBankAccount().deposit(this, amount, tax);
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
}
