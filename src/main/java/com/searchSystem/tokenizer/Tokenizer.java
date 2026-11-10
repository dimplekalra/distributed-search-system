package com.searchSystem.tokenizer;

import java.util.*;

public class Tokenizer {
    public List<String> tokenize(String text) {
        if(text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(text.toLowerCase().split("\\W+"))
                     .filter(token -> !token.isEmpty())
                     .toList();
    }
}
