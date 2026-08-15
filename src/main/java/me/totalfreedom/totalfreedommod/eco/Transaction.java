package me.totalfreedom.totalfreedommod.eco;

import java.util.Optional;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.ITransaction;
import me.totalfreedom.api.economy.TStatus;
import me.totalfreedom.api.economy.Transactional;

public class Transaction<S extends Transactional<S>,
                         R extends Transactional<R>> implements ITransaction<S, R>
{
    private final S sender;
    private final R recipient;
    private final int amount;
    private final int tax;
    private final TStatus status;
    private final Optional<IBank> taxRecipient;

    /**
     * @param taxRecipient the bank owed {@code tax}, or {@code null} if this transaction carries no tax.
     */
    protected Transaction(final S sender, final R recipient, final int amount, final TStatus status,
                           final int tax, final IBank taxRecipient)
    {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.status = status;
        this.tax = status == TStatus.SUCCESS ? tax : 0;
        this.taxRecipient = this.tax != 0 ? Optional.ofNullable(taxRecipient) : Optional.empty();
    }

    @Override
    public S sender()
    {
        return this.sender;
    }

    @Override
    public R recipient()
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
    public Optional<IBank> taxRecipient()
    {
        return this.taxRecipient;
    }

    @Override
    public int taxAmount()
    {
        return this.tax;
    }
}
