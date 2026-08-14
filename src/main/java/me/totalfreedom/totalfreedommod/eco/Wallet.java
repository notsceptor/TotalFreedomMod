package me.totalfreedom.totalfreedommod.eco;

import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.IWallet;
import me.totalfreedom.api.economy.ITransaction;

public final class Wallet implements IWallet 
{
    private final IEcoPlayer player;

    private volatile int balance;

    public Wallet(final IEcoPlayer player)
    {
        this.player = player;
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

    @Override
    public ITransaction deposit(int amount) 
    {
        final Transaction transaction = new Transaction(this, player, )

        this.balance += amount;
    }

    @Override
    public ITransaction withdraw(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'withdraw'");
    }

    @Override
    public ITransaction pay(IEcoPlayer recipient, int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'pay'");
    }

}
