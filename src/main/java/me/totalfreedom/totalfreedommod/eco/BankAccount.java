package me.totalfreedom.totalfreedommod.eco;

import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ITransaction;

public class BankAccount implements IBankAccount
{

    @Override
    public IEcoPlayer player() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'player'");
    }

    @Override
    public int checkingBalance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkingBalance'");
    }

    @Override
    public int savingsBalance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'savingsBalance'");
    }

    @Override
    public ITransaction deposit(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deposit'");
    }

    @Override
    public ITransaction moveToSavings(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'moveToSavings'");
    }

    @Override
    public ITransaction moveFromSavings(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'moveFromSavings'");
    }

    @Override
    public ITransaction withdraw(int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'withdraw'");
    }
        
}
