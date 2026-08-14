package me.totalfreedom.totalfreedommod.eco;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ILoan;
import me.totalfreedom.api.economy.ITransaction;

public class Bank implements IBank 
{
    private final IBankAccount bankNative;
    private final Map<IEcoPlayer, IBankAccount> playerAccounts;

    public Bank(final FreedomAPI plugin)
    {

    }

    @Override
    public IBankAccount<IBank> bankNative() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bankNative'");
    }

    @Override
    public Map<IEcoPlayer, IBankAccount<IEcoPlayer>> getPlayerBankAccounts() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerBankAccounts'");
    }

    @Override
    public IBankAccount<IEcoPlayer> getBankAccount(IEcoPlayer player) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBankAccount'");
    }

    @Override
    public ILoan loan(IEcoPlayer recipient, int amount, int interestRate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loan'");
    }

    @Override
    public ITransaction<IEcoPlayer, IBank> loanPayment(IEcoPlayer sender, int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loanPayment'");
    }
    

    
    
}
