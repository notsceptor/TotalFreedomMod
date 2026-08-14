package me.totalfreedom.totalfreedommod.eco;

import java.util.Optional;

import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.api.economy.ITransaction;
import me.totalfreedom.api.economy.TStatus;
import me.totalfreedom.api.economy.Transactional;

public abstract class Transaction<S extends Transactional, 
                                  R extends Transactional> implements ITransaction<S, R>
{
    private final S sender;
    private final R recipient;
    private final int amount;
    private final Optional<IBank> bank;

    protected Transaction(final S sender, final R recipient, final int amount)
    {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
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
    public int amount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'amount'");
    }

    @Override
    public TStatus status() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'status'");
    }

    @Override
    public Optional<IBank> taxRecipient() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'taxRecipient'");
    }

    @Override
    public int taxAmount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'taxAmount'");
    }
}
