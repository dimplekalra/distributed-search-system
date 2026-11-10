package com.searchSystem.service.search;

import com.searchSystem.autoComplete.Trie;
import com.searchSystem.model.Suggestion;
import com.searchSystem.service.QueryTracker;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AutoCompleteService {
    private final Trie trie;
    private final QueryTracker queryTracker;

    public AutoCompleteService(Trie trie, QueryTracker queryTracker) {
        this.trie = trie;
        this.queryTracker = queryTracker;
    }

    public List<Suggestion> suggest(String prefix, int limit) {
        List<String> matches = trie.searchPrefix(prefix.toLowerCase());

        List<Suggestion> suggestions = new ArrayList<>();

        for (String query : matches) {
            suggestions.add(new Suggestion(query, queryTracker.getFrequency(query)));
        }

        suggestions.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));

        if (suggestions.size() > limit) {
            return suggestions.subList(0, limit);
        }

        return suggestions;
    }
}
