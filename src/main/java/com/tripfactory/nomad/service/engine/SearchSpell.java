package com.tripfactory.nomad.service.engine;

import java.util.Collection;
import java.util.Locale;

import com.tripfactory.nomad.service.util.CityAliasResolver;

/**
 * Helps a search that found nothing: knows the common city nicknames (Bangalore, Vizag) and fixes small typos
 * ("lalbag", "cubon park") by comparing each word with the words that really exist in the data.
 */
public final class SearchSpell {

    private SearchSpell() {
    }

    /**
     * @return the corrected text in lower case, or null if nothing needed or could be fixed
     */
    public static String correct(String text, Collection<String> vocabulary) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.trim().toLowerCase(Locale.ROOT);

        // A known nickname for a whole city: bangalore -> bengaluru
        String canonical = CityAliasResolver.canonicalize(lower);
        if (!canonical.equalsIgnoreCase(lower)) {
            return canonical.toLowerCase(Locale.ROOT);
        }

        StringBuilder out = new StringBuilder();
        boolean changed = false;
        for (String word : lower.split("\\s+")) {
            String fixed = word;
            if (word.length() >= 4 && vocabulary.stream().noneMatch(v -> v.startsWith(word))) {
                String best = null;
                int bestDistance = Integer.MAX_VALUE;
                int allowed = word.length() <= 6 ? 1 : 2;
                for (String v : vocabulary) {
                    // Same first letter and similar length keeps wrong guesses rare and the loop cheap.
                    if (v.isEmpty() || v.charAt(0) != word.charAt(0) || Math.abs(v.length() - word.length()) > allowed) {
                        continue;
                    }
                    int d = distance(word, v);
                    if (d <= allowed && d < bestDistance) {
                        bestDistance = d;
                        best = v;
                    }
                }
                if (best != null) {
                    fixed = best;
                    changed = true;
                }
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(fixed);
        }
        return changed ? out.toString() : null;
    }

    /** Edit distance where swapping two neighbouring letters counts as one mistake. */
    static int distance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }
}
