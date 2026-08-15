package me.totalfreedom.totalfreedommod.eco;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.ITransaction;
import me.totalfreedom.api.economy.IWallet;
import me.totalfreedom.api.economy.Transactional;

public final class EcoPlayer implements IEcoPlayer
{
    private final String name;
    private final UUID uuid;
    private final Wallet wallet;
    private final BankAccount<IEcoPlayer> bankAccount;

    public EcoPlayer(final String name, final UUID uuid)
    {
        this(name, uuid, 0, 0, 0);
    }

    EcoPlayer(final String name, final UUID uuid, final int walletBalance, final int checkingBalance, final int savingsBalance)
    {
        this.name = name;
        this.uuid = uuid;
        this.wallet = new Wallet(this, walletBalance);
        this.bankAccount = new BankAccount<>(this, checkingBalance, savingsBalance);
    }

    /**
     * Overwrites this player's live balances from a freshly-loaded snapshot. Used to reconcile
     * a player who joined before the database finished connecting.
     */
    void applySnapshot(final EcoPlayerData data)
    {
        wallet.setBalance(data.walletBalance());
        bankAccount.updateChecking(data.checkingBalance());
        bankAccount.updateSavings(data.savingsBalance());
    }

    @Override
    public String name()
    {
        return this.name;
    }

    @Override
    public UUID uuid()
    {
        return this.uuid;
    }

    @Override
    public IWallet getWallet()
    {
        return this.wallet;
    }

    @Override
    public IBankAccount<IEcoPlayer> getBankAccount()
    {
        return this.bankAccount;
    }

    @Override
    public Optional<Player> bukkit()
    {
        return Optional.ofNullable(Bukkit.getPlayer(uuid))
                       .or(() -> Optional.ofNullable(Bukkit.getPlayer(name)));
    }

    @Override
    public <S extends Transactional<S>> ITransaction<S, IEcoPlayer> deposit(final S sender, final int amount, final int tax)
    {
        final ITransaction<S, IWallet> result = getWallet().deposit(sender, amount, tax);
        return new Transaction<>(sender, 
                                 this, 
                                 result.amount(), 
                                 result.status(), 
                                 result.taxAmount(), 
                                 result.taxRecipient().orElse(null));
    }

    @Override
    public <R extends Transactional<R>> ITransaction<IEcoPlayer, R> withdraw(final R recipient, final int amount, final int tax)
    {
        final ITransaction<IWallet, R> result = getWallet().withdraw(recipient, amount, tax);
        return new Transaction<>(this, 
                                 recipient, 
                                 result.amount(), 
                                 result.status(), 
                                 result.taxAmount(), 
                                 result.taxRecipient().orElse(null));
    }
}
