package me.totalfreedom.totalfreedommod.eco;

import java.util.Map;
import java.util.stream.Collectors;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ILoan;
import me.totalfreedom.api.economy.ILoan.Interval;
import me.totalfreedom.api.economy.ITransaction;
import me.totalfreedom.api.economy.Transactional;

public class Bank implements IBank
{
    private final EcoManager ecoManager;
    private final BankAccount<IBank> bankNative;

    public Bank(final EcoManager ecoManager)
    {
        this(ecoManager, 0, 0);
    }

    Bank(final EcoManager ecoManager, final int initialChecking, final int initialSavings)
    {
        this.ecoManager = ecoManager;
        this.bankNative = new BankAccount<>(this, initialChecking, initialSavings);
    }

    @Override
    public IBankAccount<IBank> bankNative()
    {
        return this.bankNative;
    }

    /**
     * Overwrites the bank's live native-account balances from a freshly-loaded snapshot. Used
     * to reconcile a bank that started up before the database finished connecting.
     */
    void applySnapshot(final EcoBankData data)
    {
        bankNative.updateChecking(data.checkingBalance());
        bankNative.updateSavings(data.savingsBalance());
    }

    @Override
    public Map<IEcoPlayer, IBankAccount<IEcoPlayer>> getPlayerBankAccounts()
    {
        return ecoManager.getLoadedPlayers()
                         .stream()
                         .collect(Collectors.toMap(player -> player, IEcoPlayer::getBankAccount));
    }

    @Override
    public IBankAccount<IEcoPlayer> getBankAccount(final IEcoPlayer player)
    {
        return player.getBankAccount();
    }

    @Override
    public ILoan loan(final IEcoPlayer recipient, final int amount, final int interestRate, final Interval interval)
    {
        return new BankLoan(this, recipient, amount, interestRate, interval);
    }

    @Override
    public ITransaction<IEcoPlayer, IBank> loanPayment(final IEcoPlayer sender, final int amount)
    {
        final ITransaction<IBankAccount<IEcoPlayer>, IBank> result = getBankAccount(sender).wire(this, amount, 0);
        return new Transaction<>(sender, 
                                 this, 
                                 result.amount(), 
                                 result.status(), 
                                 result.taxAmount(), 
                                 result.taxRecipient().orElse(null));
    }

    @Override
    public <S extends Transactional<S>> ITransaction<S, IBank> deposit(final S sender, final int amount, final int tax)
    {
        final ITransaction<S, IBankAccount<IBank>> result = bankNative.deposit(sender, amount, tax);
        return new Transaction<>(sender, 
                                 this, 
                                 result.amount(), 
                                 result.status(),
                                 result.taxAmount(), 
                                 result.taxRecipient().orElse(null));
    }

    @Override
    public <R extends Transactional<R>> ITransaction<IBank, R> withdraw(final R recipient, final int amount, final int tax)
    {
        final ITransaction<IBankAccount<IBank>, R> result = bankNative.wire(recipient, amount, tax);
        return new Transaction<>(this, 
                                 recipient, 
                                 result.amount(), 
                                 result.status(),
                                 result.taxAmount(), 
                                 result.taxRecipient().orElse(null));
    }
}
