package me.totalfreedom.totalfreedommod.blocking.sweep;

/** Why a chunk visit was scheduled. */
public enum SweepContext
{
    INITIAL("initial"),
    PERIODIC("periodic-sweep"),
    CHUNK_LOAD("chunk-load");

    private final String label;

    SweepContext(String label)
    {
        this.label = label;
    }

    public String label()
    {
        return label;
    }
}
