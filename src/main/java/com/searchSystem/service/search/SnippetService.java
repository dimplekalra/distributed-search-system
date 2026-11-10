package com.searchSystem.service.search;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SnippetService {
    private static final int SNIPPET_RADIUS = 40;

    public String generateSnippet(
            String content,
            List<String> queryTerms) {

        if (content == null || content.isEmpty()) {
            return "";
        }

        String lowerContent = content.toLowerCase();

        int firstMatchIndex = -1;

        String matchedTerm = null;

        for (String term : queryTerms) {

            int idx = lowerContent.indexOf(
                    term.toLowerCase());

            if (idx != -1) {

                firstMatchIndex = idx;

                matchedTerm = term;

                break;
            }
        }

        if (firstMatchIndex == -1) {

            return content.length() <= 120
                    ? content
                    : content.substring(0, 120) + "...";
        }

        int start = Math.max(
                0,
                firstMatchIndex - SNIPPET_RADIUS);

        int end = Math.min(
                content.length(),
                firstMatchIndex + matchedTerm.length()
                        + SNIPPET_RADIUS);

        String snippet = content.substring(start, end);

        for (String term : queryTerms) {

            snippet = snippet.replaceAll(
                    "(?i)" + term,
                    "<b>" + term + "</b>");
        }

        if (start > 0) {
            snippet = "..." + snippet;
        }

        if (end < content.length()) {
            snippet = snippet + "...";
        }

        return snippet;
    }
}
