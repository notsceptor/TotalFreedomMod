package me.totalfreedom.totalfreedommod.blocking.item;

enum ContainerSweepPolicy
{

    FILTER,
    CLEAR,
    DESTROY;

    enum Action
    {
        FILTER_SLOT,
        CLEAR_CONTAINER,
        DESTROY_BLOCK
    }

    static ContainerSweepPolicy fromConfig(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return CLEAR;
        }
        return switch (raw.trim().toLowerCase().replace('-', '_'))
        {
            case "filter" -> FILTER;
            case "destroy" -> DESTROY;
            case "clear" -> CLEAR;
            default -> CLEAR;
        };
    }

    Action actionFor(ItemScanner.Reason reason)
    {
        if (!reason.isHangClass())
        {
            return Action.FILTER_SLOT;
        }
        return switch (this)
        {
            case FILTER -> Action.FILTER_SLOT;
            case CLEAR -> Action.CLEAR_CONTAINER;
            case DESTROY -> Action.DESTROY_BLOCK;
        };
    }
}
