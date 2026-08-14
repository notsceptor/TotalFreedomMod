package me.totalfreedom.totalfreedommod.eco;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.totalfreedom.api.economy.IBankAccount;
import me.totalfreedom.api.economy.IEcoPlayer;
import me.totalfreedom.api.economy.IWallet;
import me.totalfreedom.api.economy.ITransaction;

public final class EcoPlayer implements IEcoPlayer 
{
    private final IWallet wallet;
    private final IBankAccount bankAccount;
    private final String name;
    private final UUID uuid;

    public EcoPlayer()
    {
        
    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'name'");
    }

    @Override
    public UUID uuid() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'uuid'");
    }

    @Override
    public IWallet getWallet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWallet'");
    }

    @Override
    public IBankAccount getBankAccount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBankAccount'");
    }

    @Override
    public Optional<Player> bukkit() 
    {
        return Optional.ofNullable(Bukkit.getPlayer(uuid))
                                         .or(() -> Optional.ofNullable(Bukkit.getPlayer(name)));
    }

    @Override
    public ITransaction pay(IEcoPlayer recipient, int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'pay'");
    }
}
