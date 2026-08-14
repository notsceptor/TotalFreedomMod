package me.totalfreedom.api.economy;

import java.util.Optional;

public interface ILoan extends ITransaction<IBank, IEcoPlayer> 
{
    int interestRate();

    Interval payInterval();

    @Override
    default Optional<IBank> taxRecipient()
    {
        return Optional.empty();
    }

    @Override
    default int taxAmount()
    {
        return 0;
    }

    public static enum Interval
    {
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY,
        QUARTERLY,
        SEMIANNUAL,
        ANNUAL;
    }
}
