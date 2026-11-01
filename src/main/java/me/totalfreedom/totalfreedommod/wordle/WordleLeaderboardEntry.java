package me.totalfreedom.totalfreedommod.wordle;

import java.util.UUID;

public record WordleLeaderboardEntry(UUID playerId, String playerName, int guesses, boolean won)
{
}
