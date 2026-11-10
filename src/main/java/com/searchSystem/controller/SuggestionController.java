package com.searchSystem.controller;

import com.searchSystem.autoComplete.Trie;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/suggest")
public class SuggestionController {
    private final Trie trie;

    public SuggestionController(Trie trie) {
        this.trie = trie;
    }

    @GetMapping
    public List<String> suggest(@RequestParam String prefix) {
        return trie.searchPrefix(prefix);
    }
}
