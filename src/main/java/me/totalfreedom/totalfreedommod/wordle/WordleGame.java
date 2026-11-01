package me.totalfreedom.totalfreedommod.wordle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WordleGame
{
    static final int WORD_LENGTH = 5;
    static final int MAX_GUESSES = 6;

    private WordleGame()
    {
    }
    
    public static List<LetterResult> score(final String answer, final String guess)
    {
        final LetterResult[] result = new LetterResult[WORD_LENGTH];
        final Map<Character, Integer> remainingLetters = new HashMap<>();

        for (int index = 0; index < WORD_LENGTH; index++)
        {
            final char answerLetter = answer.charAt(index);
            final char guessLetter = guess.charAt(index);

            if (guessLetter == answerLetter)
                result[index] = LetterResult.GREEN;
            else
                remainingLetters.merge(answerLetter, 1, Integer::sum);
        }

        for (int index = 0; index < WORD_LENGTH; index++)
        {
            if (result[index] == LetterResult.GREEN)
                continue;

            final char guessLetter = guess.charAt(index);
            final int remaining = remainingLetters.getOrDefault(guessLetter, 0);

            if (remaining > 0)
            {
                result[index] = LetterResult.ORANGE;
                remainingLetters.put(guessLetter, remaining - 1);
            }
            else
                result[index] = LetterResult.GRAY;
        }

        return List.of(result);
    }

    static boolean isValidWord(final String word)
    {
        return word != null && word.matches("[A-Z]{" + WORD_LENGTH + "}");
    }

    public enum LetterResult
    {
        GREEN,
        ORANGE,
        GRAY
    }
}
