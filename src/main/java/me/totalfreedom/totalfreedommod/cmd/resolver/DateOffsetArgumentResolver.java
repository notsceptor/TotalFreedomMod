package me.totalfreedom.totalfreedommod.cmd.resolver;

import me.totalfreedom.totalfreedommod.util.FUtil;

import java.util.Date;

public class DateOffsetArgumentResolver implements AbstractArgumentResolver<Date>
{

    @Override
    public String name()
    {
        return "DateOffset";
    }

    @Override
    public Date resolve(String arg, String strategy)
    {
        final Date date = FUtil.parseDateOffset(arg);

        if (date == null)
        {
            throw new ArgumentResolutionException("Invalid duration: " + arg);
        }

        return date;
    }
}
