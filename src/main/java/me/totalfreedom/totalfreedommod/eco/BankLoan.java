package me.totalfreedom.totalfreedommod.eco;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ILoan;
import me.totalfreedom.api.economy.TStatus;

public class BankLoan implements ILoan
{
    private final IBank bank;
    private final IEcoPlayer recipient;
    private final int amount;
    private final int interestRate;
    private final Interval paymentInterval;
    private final TStatus status;

    public BankLoan(final IBank bank, final IEcoPlayer recipient, final int amount, final int interestRate, final Interval paymentInterval)
    {
        this.bank = bank;
        this.recipient = recipient;
        this.amount = amount;
        this.interestRate = interestRate;
        this.paymentInterval = paymentInterval;

        if (amount <= 0)
        {
            this.status = TStatus.INVALID_AMOUNT;
            return;
        }

        if (bank.bankNative().checkingBalance() < amount)
        {
            this.status = TStatus.INSUFFICIENT_FUNDS;
            return;
        }

        bank.bankNative().wire(recipient, amount, 0);
        this.status = TStatus.SUCCESS;
    }

    @Override
    public IBank sender()
    {
        return this.bank;
    }

    @Override
    public IEcoPlayer recipient()
    {
        return this.recipient;
    }

    @Override
    public int amount()
    {
        return this.amount;
    }

    @Override
    public TStatus status()
    {
        return this.status;
    }

    @Override
    public int interestRate()
    {
        return this.interestRate;
    }

    @Override
    public Interval payInterval()
    {
        return this.paymentInterval;
    }
}
