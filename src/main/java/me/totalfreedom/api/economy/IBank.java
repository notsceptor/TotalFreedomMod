package me.totalfreedom.api.economy;

import java.util.Map;

public interface IBank extends Transactional
{
    /**
     * Represents the Bank's default Bank Account.
     * <p>
     * This account can give out loans, tax payments, etc. 
     * 
     * @return An {@link IBankAccount} that is owned by the Bank itself.
     */
    IBankAccount<IBank> bankNative();
    
    Map<IEcoPlayer, IBankAccount<IEcoPlayer>> getPlayerBankAccounts();

    IBankAccount<IEcoPlayer> getBankAccount(final IEcoPlayer player);

    ILoan loan(final IEcoPlayer recipient, final int amount, final int interestRate);

    ITransaction<IEcoPlayer, IBank> loanPayment(final IEcoPlayer sender, final int amount);
}
