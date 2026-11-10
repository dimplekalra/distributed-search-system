package com.searchSystem.service.search.fuzzy;

import com.searchSystem.autoComplete.Trie;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuzzySearchService {
    private final Trie trie;

    public FuzzySearchService(
            Trie trie) {

        this.trie = trie;
    }

    public String findClosestTerm(
            String query) {

        List<String> vocabulary = trie.getAllWords();

        String bestMatch = null;

        int bestDistance = Integer.MAX_VALUE;

        for (String word : vocabulary) {

            int distance = LevenshteinDistance.compute(
                    query,
                    word);

            if (distance < bestDistance) {

                bestDistance = distance;
                bestMatch = word;
            }
        }

        if (bestDistance <= 2) {
            return bestMatch;
        }

        return null;
    }
}
