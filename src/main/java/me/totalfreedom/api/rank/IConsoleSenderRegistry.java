package me.totalfreedom.api.rank;

public interface IConsoleSenderRegistry
{
    void load();

    String getRankIdForSender(String senderName);

    boolean isWhitelisted(String senderName);
}
