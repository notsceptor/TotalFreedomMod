package me.totalfreedom.totalfreedommod.cmd.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FuzzyMatch
{
    private FuzzyMatch() {}

    public static boolean matches(String candidate, String query)
    {
        return score(candidate, query) != null;
    }

    /**
     * Filters {@code candidates} to those fuzzy-matching {@code query}, sorted by match quality
     * (tighter, earlier matches first). An empty {@code query} returns all candidates, unsorted.
     */
    public static List<String> filter(List<String> candidates, String query)
    {
        if (query.isEmpty()) return List.copyOf(candidates);

        List<Scored> scored = new ArrayList<>();
        for (String candidate : candidates)
        {
            Integer score = score(candidate, query);
            if (score != null) scored.add(new Scored(candidate, score));
        }
        scored.sort(Comparator.comparingInt(s -> s.score()));

        List<String> result = new ArrayList<>(scored.size());
        for (Scored s : scored) result.add(s.candidate());
        return result;
    }

    private static Integer score(String candidate, String query)
    {
        String c = candidate.toLowerCase();
        String q = query.toLowerCase();

        int searchFrom = 0;
        int gapPenalty = 0;
        int firstMatch = -1;
        for (int qi = 0; qi < q.length(); qi++)
        {
            int idx = c.indexOf(q.charAt(qi), searchFrom);
            if (idx < 0) return null;
            if (firstMatch < 0) firstMatch = idx;
            gapPenalty += idx - searchFrom;
            searchFrom = idx + 1;
        }
        return firstMatch + gapPenalty;
    }

    private record Scored(String candidate, int score) {}
}
