package me.totalfreedom.totalfreedommod.command.resolver;

import com.google.common.net.InetAddresses;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InetAddressResolver implements AbstractArgumentResolver<InetAddress>
{
    @Override
    public String name()
    {
        return "IP";
    }

    @Override
    public InetAddress resolve(String arg, String strategy)
    {
        if (!InetAddresses.isInetAddress(arg))
        {
            throw new ArgumentResolutionException("Invalid IP address: " + arg);
        }

        return InetAddress.ofLiteral(arg);
    }
}
