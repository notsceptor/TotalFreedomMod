package me.totalfreedom.totalfreedommod.blocking.packet;

final class CommandDepthGuard
{

    private final int maxDepth;

    CommandDepthGuard(int maxDepth)
    {
        this.maxDepth = maxDepth;
    }

    int maxDepth()
    {
        return maxDepth;
    }

    int depthOf(String text)
    {
        return maxNestingDepth(text, maxDepth);
    }

    static int maxNestingDepth(String text, int cap)
    {
        if (text == null)
        {
            return 0;
        }
        final int limit = Math.max(cap, 0);
        int depth = 0;
        int max = 0;
        for (int i = 0, n = text.length(); i < n; i++)
        {
            final char c = text.charAt(i);
            if (c == '{' || c == '[')
            {
                depth++;
                if (depth > max)
                {
                    max = depth;
                    if (max > limit)
                    {
                        return max;
                    }
                }
            }
            else if ((c == '}' || c == ']') && depth > 0)
            {
                depth--;
            }
        }
        return max;
    }
}
