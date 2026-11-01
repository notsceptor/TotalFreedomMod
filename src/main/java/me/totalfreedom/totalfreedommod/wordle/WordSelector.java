package me.totalfreedom.totalfreedommod.wordle;

import java.time.LocalDate;

@FunctionalInterface
public interface WordSelector
{
    String select(LocalDate date);
}
