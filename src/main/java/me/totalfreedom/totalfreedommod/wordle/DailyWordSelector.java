package me.totalfreedom.totalfreedommod.wordle;

import java.time.LocalDate;

final class DailyWordSelector implements WordSelector
{
    private final WordList wordList;

    DailyWordSelector(final WordList wordList)
    {
        this.wordList = wordList;
    }

    @Override
    public String select(final LocalDate date)
    {
        return wordList.select(date.toEpochDay());
    }
}
