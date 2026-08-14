package me.totalfreedom.api.economy;

/**
 * Simple interface that represents a Wallet. 
 * A wallet is assigned to a player and can only ever hold a balance. 
 * 
 * @return
 */
public interface IWallet extends Transactional
{
    IEcoPlayer getPlayer();
    
    int balance();

    ITransaction<IEcoPlayer, IWallet> deposit(final int amount);

    ITransaction<IWallet, IEcoPlayer> withdraw(final int amount);

    ITransaction<IEcoPlayer, IEcoPlayer> pay(final IEcoPlayer recipient, final int amount);
}
