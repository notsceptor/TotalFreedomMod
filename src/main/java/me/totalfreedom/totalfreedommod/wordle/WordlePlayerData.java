package me.totalfreedom.totalfreedommod.wordle;

import java.util.List;

public record WordlePlayerData(String gameDate, List<String> guesses, Status status, String lastWinDate,
                               int currentStreak, int gamesPlayed, int gamesWon)
{
    public WordlePlayerData
    {
        guesses = List.copyOf(guesses);
    }

    public enum Status
    {
        IN_PROGRESS,
        WON,
        LOST
    }
}
